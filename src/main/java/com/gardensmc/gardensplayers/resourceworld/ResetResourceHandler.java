package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.gardensmc.gardensplayers.resourceworld.GenResourceHandler.WORLDS;

/**
 * Swaps the live resource worlds for pregenerated ones (made by /genresource as {@code <name>.<uuid>}).
 * Every check happens before anything is deleted, and the file work runs off the main thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResetResourceHandler {

    public static final ResetResourceHandler INSTANCE = new ResetResourceHandler();

    private boolean resetting;

    public void triggerReset() {
        if (resetting) {
            GardensPlayers.plugin.getLogger().warning("A resource world reset is already running");
            return;
        }
        // 1. every world needs a pregenerated replacement, or nothing is touched
        var replacements = new HashMap<String, Path>();
        for (var name : WORLDS.keySet()) {
            var replacement = findPregenerated(name);
            if (replacement.isEmpty()) {
                GardensPlayers.plugin.getLogger().severe("No pregenerated world found for " + name + " - run /genresource first. Nothing was reset.");
                return;
            }
            replacements.put(name, replacement.get());
        }
        resetting = true;
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
                Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize("<red>Resource world reset failed, the old worlds are still available."));
                return;
            }
            unloaded.add(name);
        }

        // 3. swap the folders off the main thread (the worlds are unloaded, so nothing else touches them)
        var worldContainer = Bukkit.getWorldContainer().toPath();
        CompletableFuture.runAsync(() -> replacements.forEach((name, replacement) -> {
            try {
                var target = worldContainer.resolve(name);
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
            }
            WORLDS.keySet().forEach(this::createWorld);
            renderMap();
            resetting = false;
            Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize(error == null
                    ? "Resource worlds have been reset."
                    : "<red>Resource world reset ran into a problem, staff have been notified."));
        }));
    }

    // the newest folder named exactly <name>.<uuid> ("resource" must not match "resource_nether.<uuid>")
    private Optional<Path> findPregenerated(String name) {
        var pattern = Pattern.compile(Pattern.quote(name) + "\\.[0-9a-fA-F-]{36}");
        try (Stream<Path> paths = Files.list(Bukkit.getWorldContainer().toPath())) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createWorld(String name) {
        if (new WorldCreator(name).environment(WORLDS.get(name)).createWorld() == null) {
            GardensPlayers.plugin.getLogger().severe("Failed to load resource world: " + name);
        }
    }

    private void renderMap() {
        WORLDS.keySet().forEach(worldName -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "squaremap resetmap " + worldName);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "squaremap fullrender " + worldName);
        });
    }

    // teleported directly (not via /spawn, which may have a warmup) so the world is empty before unloading
    private void relocatePlayers(World world) {
        var spawn = Bukkit.getWorlds().getFirst().getSpawnLocation();
        List.copyOf(world.getPlayers()).forEach(player -> {
            player.sendMessage(MiniMessage.miniMessage().deserialize("Resource World is regenerating, moving you to spawn..."));
            player.teleport(spawn);
        });
    }
}
