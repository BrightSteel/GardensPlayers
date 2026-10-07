package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Pregenerates resource worlds for {@link ResetResourceHandler} to swap in: each is created, filled out to the resource
 * world radius by Chunky, saved and unloaded, then the next starts. One at a time, ticked every 2 seconds.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenResourceHandler {

    public static final GenResourceHandler INSTANCE = new GenResourceHandler();
    public static final Map<String, World.Environment> WORLDS = ResourceWorlds.WORLDS;

    private GenResourceStatus status = GenResourceStatus.AVAILABLE;
    // the live worlds to make a replacement for, in order
    private final Deque<String> queue = new ArrayDeque<>();
    @Nullable
    private String generating;
    private int finished;
    // whether Chunky said the world in progress is fully done (it also reports "complete" for a run that was stopped)
    private boolean filled;
    private boolean listening;
    // ticks waited since Chunky stopped for its last chunks to report in (it stops before they all have)
    private int settling;
    private static final int SETTLE_TICKS = 15; // 30 seconds at a tick every 2
    // checks in a row GardensFurniture had nothing left to place in the world (scans can still be starting up)
    private int furnitureIdle;
    private int total;
    private Audience requestor;

    /** Queues {@code count} sets: each of the three resource worlds, or only those of {@code environment}. */
    public boolean triggerGeneration(Audience requestor, int count, @Nullable World.Environment environment) {
        if (status == GenResourceStatus.GENERATING) {
            return false;
        }
        this.requestor = requestor;
        status = GenResourceStatus.GENERATING;
        finished = 0;
        for (int i = 0; i < count; i++) {
            for (var world : WORLDS.entrySet()) {
                if (environment == null || world.getValue() == environment) queue.add(world.getKey());
            }
        }
        total = queue.size();
        tick();
        return true;
    }

    /** What's happening, for /genresource status. */
    public String describe() {
        if (status != GenResourceStatus.GENERATING) {
            return "Not generating" + (status == GenResourceStatus.FAILED ? " (the last run failed, see the console)" : "") + ".";
        }
        var line = "Generating " + (finished + 1) + " of " + total + ": " + generating;
        var chunky = GardensPlayers.chunkyAPI;
        if (generating != null && chunky != null && chunky.isRunning(generating)) return line + " (Chunky is filling it)";
        int furniture = generating == null ? 0 : furniturePending(generating);
        return line + (furniture > 0 ? " (placing furniture: " + furniture + " left)" : "");
    }

    public void tick() {
        if (generating == null && queue.isEmpty()) {
            return;
        }
        var chunky = GardensPlayers.chunkyAPI;
        if (chunky == null) {
            GardensPlayers.plugin.getLogger().severe("Chunky isn't installed: can't pregenerate resource worlds");
            fail();
            return;
        }
        if (!listening) {
            chunky.onGenerationProgress(event -> {
                if (event.world().equals(generating) && (event.complete() || event.progress() >= 100f)) filled = true;
            });
            listening = true;
        }
        if (generating == null) {
            var world = queue.poll();
            generating = uniqueName(world);
            filled = false;
            settling = 0;
            furnitureIdle = 0;
            var created = new WorldCreator(generating).environment(WORLDS.get(world)).createWorld();
            if (created == null) {
                GardensPlayers.plugin.getLogger().severe("Failed to create resource world " + generating + ", stopping generation");
                fail();
                return;
            }
            // until it's finished, a reset mustn't take it (a crash or restart would leave it half made)
            ResourceWorlds.markUnfinished(created.getWorldPath(), true);
            var radius = GardensPlayers.config.getResourceWorldRadius() + 250;
            if (!chunky.startTask(generating, "square", 0, 0, radius, radius, "concentric")) {
                GardensPlayers.plugin.getLogger().severe("Chunky wouldn't start on " + generating + ", stopping generation");
                Bukkit.unloadWorld(created, true);
                fail();
                return;
            }
            GardensPlayers.plugin.getLogger().info("Pregenerating " + generating + " (" + (finished + 1) + "/" + total + ")");
        } else if (!chunky.isRunning(generating) && !filled && settling++ < SETTLE_TICKS) {
            // Chunky's task ends a moment before its last chunks finish and the 100% report arrives
        } else if (!chunky.isRunning(generating) && filled && furnitureIdle < 3) {
            // GardensFurniture swaps world-gen marker blocks for furniture as chunks generate; anything still queued
            // when the world unloads would be dropped for good, so wait until it's had nothing to do a few times
            furnitureIdle = furniturePending(generating) == 0 ? furnitureIdle + 1 : 0;
        } else if (!chunky.isRunning(generating)) {
            // saved on the way out: Chunky's chunks may not all be on disk yet
            var done = generating;
            var world = Bukkit.getWorld(done);
            var folder = world == null ? ResourceWorlds.dimensions().resolve(done) : world.getWorldPath();
            if (!Bukkit.unloadWorld(done, true)) {
                GardensPlayers.plugin.getLogger().severe("Failed to unload " + done + " after generating it");
            }
            generating = null;
            if (!filled) {
                // stopped before it was done (chunky cancel, an error): keep it out of resets
                GardensPlayers.plugin.getLogger().severe("Chunky stopped before " + done + " was finished; it's left marked unfinished and won't be used");
                fail();
                return;
            }
            ResourceWorlds.markUnfinished(folder, false);
            finished++;
            GardensPlayers.plugin.getLogger().info("Pregenerated " + done + " (" + finished + "/" + total + ")");
            if (queue.isEmpty()) {
                status = GenResourceStatus.AVAILABLE;
                tell("<green>Finished generating " + total + " resource worlds. Resets ready: " + ResourceWorlds.resetsLeft());
            } else {
                tick();
            }
        }
    }

    private static int furniturePending(String world) {
        return com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader.INSTANCE.pending(world);
    }

    // when, to the second; two in the same second (not in practice: each takes minutes) get a suffix
    private static String uniqueName(String world) {
        var name = ResourceWorlds.newName(world);
        var dir = ResourceWorlds.dimensions();
        var candidate = name;
        for (int n = 2; Files.exists(dir.resolve(candidate)) || Bukkit.getWorld(candidate) != null; n++) {
            candidate = name + "-" + n;
        }
        return candidate;
    }

    private void fail() {
        status = GenResourceStatus.FAILED;
        generating = null;
        queue.clear();
        tell("<red>Resource world generation stopped, see the console.");
    }

    private void tell(String message) {
        if (requestor != null) requestor.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }
}
