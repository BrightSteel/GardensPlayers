package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.gardensmc.gardensplayers.resourceworld.ResourceWorlds.WORLDS;

/**
 * Swaps the live resource worlds for the oldest pregenerated ones (see {@link ResourceWorlds}). Every check happens
 * before anything is deleted, and the file work runs off the main thread. Meant to run on the first of each month
 * ({@code /resetresource} from the console or a scheduler).
 *
 * <p>Running out is caught early: staff hear about it when fewer than {@value #WARN_AT} resets are left (at startup,
 * on joining and after each reset), and a reset with a world missing doesn't touch anything.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResetResourceHandler {

    public static final ResetResourceHandler INSTANCE = new ResetResourceHandler();
    public static final String PERMISSION = "gplayers.resetresource";
    /** Below this many resets left, staff are warned. */
    public static final int WARN_AT = 3;

    // players who logged out in a resource world remember which reset it was, so after a newer one they're moved
    private static final NamespacedKey SEEN_RESET = new NamespacedKey("gardensplayers", "resource_reset_seen");

    private boolean resetting;

    /** Loads the live resource worlds that aren't loaded yet (this plugin owns them, not Multiverse). */
    public void loadWorlds() {
        for (var name : WORLDS.keySet()) {
            if (Bukkit.getWorld(name) == null && Files.isDirectory(ResourceWorlds.dimensions().resolve(name))) {
                createWorld(name);
            }
        }
    }

    public void triggerReset(CommandSender sender) {
        if (resetting) {
            tell(sender, "<red>A resource world reset is already running.");
            return;
        }
        // 1. every world needs a pregenerated replacement, or nothing is touched
        var replacements = new HashMap<String, Path>();
        for (var name : WORLDS.keySet()) {
            var available = ResourceWorlds.available(name);
            if (available.isEmpty()) {
                var message = "No pregenerated " + name + " left: nothing was reset. Run /genresource to make more.";
                GardensPlayers.plugin.getLogger().severe(message);
                tell(sender, "<red>" + message);
                warnStaff("<red>The resource world reset failed: no pregenerated " + name + " left. Run /genresource.");
                return;
            }
            replacements.put(name, available.getFirst().folder());
        }
        resetting = true;
        GardensPlayers.plugin.getLogger().info("Resetting resource worlds with " + replacements.values().stream().map(p -> p.getFileName().toString()).toList());
        Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize("Resource worlds are resetting, they will be temporarily unavailable"));

        // 2. move players out and unload; if any world won't unload, put the others back and stop
        var unloaded = new ArrayList<String>();
        for (var name : WORLDS.keySet()) {
            var world = Bukkit.getWorld(name);
            if (world == null) {
                continue;
            }
            relocatePlayers(world);
            if (!Bukkit.unloadWorld(world, false)) {
                GardensPlayers.plugin.getLogger().severe("Failed to unload resource world " + name + " - reset cancelled, nothing was deleted");
                unloaded.forEach(this::createWorld);
                resetting = false;
                tell(sender, "<red>The reset was cancelled: " + name + " wouldn't unload. Nothing was deleted.");
                Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize("<red>Resource world reset failed, the old worlds are still available."));
                return;
            }
            unloaded.add(name);
        }

        // 3. swap the folders off the main thread (the worlds are unloaded, so nothing else touches them)
        var dimensions = ResourceWorlds.dimensions();
        CompletableFuture.runAsync(() -> replacements.forEach((name, replacement) -> {
            try {
                var target = dimensions.resolve(name);
                if (Files.exists(target)) {
                    FileUtils.deleteDirectory(target.toFile());
                }
                Files.move(replacement, target);
            } catch (IOException e) {
                throw new RuntimeException("Failed to swap in the new " + name + " world", e);
            }
        })).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            // 4. load whatever is in place now - the new worlds, or on failure whatever is left
            if (error != null) {
                GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Resource world reset failed while swapping folders", error);
            } else {
                recordReset();
            }
            WORLDS.keySet().forEach(this::createWorld);
            renderMap();
            resetting = false;
            Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize(error == null
                    ? "Resource worlds have been reset."
                    : "<red>Resource world reset ran into a problem, staff have been notified."));
            int left = ResourceWorlds.resetsLeft();
            tell(sender, (error == null ? "<green>Reset done." : "<red>Reset failed while swapping folders, see the console.") + " <gray>Resets left: <white>" + left);
            GardensPlayers.plugin.getLogger().info("Resource world resets left: " + left);
            checkSupply();
        }));
    }

    // ---- running out ----

    /** Warns the console and online staff when few resets are left. */
    public void checkSupply() {
        var unfinished = ResourceWorlds.unfinished();
        if (!unfinished.isEmpty() && GenResourceHandler.INSTANCE.describe().startsWith("Not generating")) {
            GardensPlayers.plugin.getLogger().warning("Half-made pregenerated resource worlds (skipped by resets, safe to delete): " + unfinished);
        }
        int left = ResourceWorlds.resetsLeft();
        if (left >= WARN_AT) return;
        var message = left == 0
                ? "No pregenerated resource worlds left: the next reset will fail. Run /genresource 3 (or more)."
                : "Only " + left + " resource world reset" + (left == 1 ? "" : "s") + " left. Run /genresource to make more.";
        GardensPlayers.plugin.getLogger().warning(message);
        warnStaff("<gold>" + message);
    }

    /** For a joining staff member: the same warning, just to them. */
    public void warnOnJoin(Player player) {
        if (!player.hasPermission(PERMISSION)) return;
        int left = ResourceWorlds.resetsLeft();
        if (left < WARN_AT) {
            tell(player, "<gold>" + (left == 0 ? "No pregenerated resource worlds left: the next reset will fail."
                    : "Only " + left + " resource world reset" + (left == 1 ? "" : "s") + " left.") + " <gray>Run /genresource.");
        }
    }

    /** /resetresource status: the pregenerated worlds waiting, oldest (next) first. */
    public void status(CommandSender sender) {
        tell(sender, "<gold>Resource world resets left: <white>" + ResourceWorlds.resetsLeft()
                + " <gray>(last reset: " + lastReset() + ")");
        for (var name : WORLDS.keySet()) {
            var available = ResourceWorlds.available(name);
            tell(sender, " <gray>" + name + ": <white>" + available.size() + (available.isEmpty() ? "" : " <gray>next: " + available.getFirst().name()));
        }
        var unfinished = ResourceWorlds.unfinished();
        if (!unfinished.isEmpty()) {
            tell(sender, " <red>Half-made (skipped, safe to delete): <white>" + String.join(", ", unfinished));
        }
        tell(sender, " <gray>" + GenResourceHandler.INSTANCE.describe());
    }

    private static void warnStaff(String message) {
        for (var player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(PERMISSION)) tell(player, message);
        }
    }

    // ---- players left inside a reset world ----

    /** A player logging out in a resource world: remember which reset that world came from. */
    public void onQuit(Player player) {
        if (WORLDS.containsKey(player.getWorld().getName())) {
            player.getPersistentDataContainer().set(SEEN_RESET, PersistentDataType.INTEGER, resetCount());
        } else {
            player.getPersistentDataContainer().remove(SEEN_RESET);
        }
    }

    /** A player joining in a resource world that's been reset since they left: they'd be inside new terrain. */
    public void onJoin(Player player) {
        var seen = player.getPersistentDataContainer().get(SEEN_RESET, PersistentDataType.INTEGER);
        if (seen != null && seen != resetCount() && WORLDS.containsKey(player.getWorld().getName())) {
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            tell(player, "<gray>The resource worlds were reset while you were away, so you've been moved to spawn.");
        }
        player.getPersistentDataContainer().remove(SEEN_RESET);
    }

    private File stateFile() {
        return new File(GardensPlayers.plugin.getDataFolder(), "resource-resets.yml");
    }

    private int resetCount() {
        return YamlConfiguration.loadConfiguration(stateFile()).getInt("count", 0);
    }

    private String lastReset() {
        return YamlConfiguration.loadConfiguration(stateFile()).getString("last", "never");
    }

    private void recordReset() {
        var yaml = YamlConfiguration.loadConfiguration(stateFile());
        yaml.set("count", yaml.getInt("count", 0) + 1);
        yaml.set("last", Instant.now().toString());
        try {
            yaml.save(stateFile());
        } catch (IOException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't save resource-resets.yml", e);
        }
    }

    // ---- helpers ----

    private void createWorld(String name) {
        World.Environment environment = WORLDS.get(name);
        if (new WorldCreator(name).environment(environment).createWorld() == null) {
            GardensPlayers.plugin.getLogger().severe("Failed to load resource world: " + name);
        }
    }

    private void renderMap() {
        if (Bukkit.getPluginManager().getPlugin("squaremap") == null) return;
        WORLDS.keySet().forEach(worldName -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "squaremap resetmap " + worldName);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "squaremap fullrender " + worldName);
        });
    }

    // teleported directly (not via /spawn, which may have a warmup) so the world is empty before unloading
    private void relocatePlayers(World world) {
        var spawn = Bukkit.getWorlds().getFirst().getSpawnLocation();
        List.copyOf(world.getPlayers()).forEach(player -> {
            tell(player, "Resource World is regenerating, moving you to spawn...");
            player.teleport(spawn);
        });
    }

    private static void tell(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }
}
