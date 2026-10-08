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
    /**
     * Chunky is paused while this many world-gen furniture blocks wait to be placed in the world, and resumed once
     * they're down to {@link #FURNITURE_RESUME}: in the End, generation is far faster than furniture placement, so this
     * is what paces it, and keeps the chunks waiting for furniture (held in memory) to a couple of thousand.
     */
    // Resumed well before the queue is empty, so generation and placement overlap instead of taking turns. Memory stays
    // bounded regardless: GardensFurniture holds at most 2,000 chunks for their blocks, and the memory check below.
    static final int FURNITURE_PAUSE = 1_000_000;
    static final int FURNITURE_RESUME = 500_000;
    /**
     * The same for the server keeping up at all: paused while the world has this many chunks loaded (Chunky only keeps
     * a few hundred in flight, so a pile-up means the main thread isn't getting to unload them) or memory is this full,
     * resumed once both are back down.
     */
    static final int CHUNKS_PAUSE = 20_000;
    static final int CHUNKS_RESUME = 4_000;
    static final double MEMORY_PAUSE = 0.80;
    static final double MEMORY_RESUME = 0.60;
    // we paused Chunky (isRunning is false while paused, which mustn't read as finished)
    private boolean paused;
    private int total;
    private Audience requestor;

    public boolean isGenerating() {
        return status == GenResourceStatus.GENERATING;
    }

    /**
     * Queues {@code count} of each world of {@code environment}; or, with none, enough to make {@code count} more full
     * sets than there are now, counting what's waiting on disk and in storage. A run stopped partway (say after the
     * End) left some worlds ahead of the others: those are topped up first, and nothing is made that has no set to go
     * in. Returns how many of each world were queued (in order), or null if a run is already going.
     */
    @Nullable
    public Map<String, Integer> triggerGeneration(Audience requestor, int count, @Nullable World.Environment environment) {
        if (status == GenResourceStatus.GENERATING) {
            return null;
        }
        var have = new java.util.LinkedHashMap<String, Integer>();
        for (var world : WORLDS.entrySet()) {
            if (environment == null || world.getValue() == environment) {
                have.put(world.getKey(), environment == null ? ResetResourceHandler.waiting(world.getKey()) : 0);
            }
        }
        var make = toMake(have, count);
        // set by set, so the first sets are whole before later ones start
        int most = make.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int i = 0; i < most; i++) {
            for (var world : make.entrySet()) {
                if (world.getValue() > i) queue.add(world.getKey());
            }
        }
        total = queue.size();
        if (total == 0) return make;
        this.requestor = requestor;
        status = GenResourceStatus.GENERATING;
        finished = 0;
        tick();
        return make;
    }

    /** How many of each world make {@code sets} more full sets than the fewest of them make now. */
    static Map<String, Integer> toMake(Map<String, Integer> have, int sets) {
        int target = have.values().stream().mapToInt(Integer::intValue).min().orElse(0) + sets;
        var make = new java.util.LinkedHashMap<String, Integer>();
        have.forEach((world, n) -> make.put(world, Math.max(0, target - n)));
        return make;
    }

    /** What's happening, for /genresource status. */
    public String describe() {
        if (status != GenResourceStatus.GENERATING) {
            return "Not generating" + (status == GenResourceStatus.FAILED ? " (the last run failed, see the console)" : "") + ".";
        }
        var line = "Generating " + (finished + 1) + " of " + total + ": " + generating;
        var chunky = GardensPlayers.chunkyAPI;
        if (paused) return line + " (Chunky paused for the server to catch up: " + furniturePending(generating) + " furniture blocks, "
                + loadedChunks(generating) + " chunks loaded, " + Math.round(memoryUsed() * 100) + "% memory)";
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
            paused = false;
            var created = new WorldCreator(generating).environment(WORLDS.get(world)).createWorld();
            if (created == null) {
                GardensPlayers.plugin.getLogger().severe("Failed to create resource world " + generating + ", stopping generation");
                fail();
                return;
            }
            // until it's finished, a reset mustn't take it (a crash or restart would leave it half made)
            ResourceWorlds.markUnfinished(created.getWorldPath(), true);
            var radius = GardensPlayers.config.resourceRadius(generating) + 250;
            if (!chunky.startTask(generating, "square", 0, 0, radius, radius, "concentric")) {
                GardensPlayers.plugin.getLogger().severe("Chunky wouldn't start on " + generating + ", stopping generation");
                Bukkit.unloadWorld(created, true);
                fail();
                return;
            }
            GardensPlayers.plugin.getLogger().info("Pregenerating " + generating + " (" + (finished + 1) + "/" + total + ")");
        } else if (paused) {
            if (furniturePending(generating) <= FURNITURE_RESUME && loadedChunks(generating) <= CHUNKS_RESUME && memoryUsed() <= MEMORY_RESUME) {
                if (chunky.continueTask(generating)) {
                    paused = false;
                    GardensPlayers.plugin.getLogger().info("Resuming Chunky on " + generating + ": the server has caught up");
                } else {
                    GardensPlayers.plugin.getLogger().severe("Chunky wouldn't resume " + generating + ", stopping generation");
                    fail();
                }
            }
        } else if (chunky.isRunning(generating) && overloaded(generating) != null) {
            var why = overloaded(generating);
            if (chunky.pauseTask(generating)) {
                paused = true;
                GardensPlayers.plugin.getLogger().warning("Pausing Chunky on " + generating + ": " + why);
            }
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
            // the generating machine sends it to storage (and frees the disk) while the next one generates
            ResourceWorldStorage.INSTANCE.uploadFinished();
            finished++;
            GardensPlayers.plugin.getLogger().info("Pregenerated " + done + " (" + finished + "/" + total + ")");
            if (queue.isEmpty()) {
                status = GenResourceStatus.AVAILABLE;
                tell("<green>Finished generating " + total + " resource worlds."
                        + (ResourceWorldStorage.INSTANCE.enabled() && GardensPlayers.config.isStorageUploadGenerated() ? " They're uploading to storage (/resetresource status)." : ""));
            } else {
                tick();
            }
        }
    }

    // why generation should wait for the server to catch up, or null if it needn't
    @Nullable
    private static String overloaded(String world) {
        int furniture = furniturePending(world);
        if (furniture >= FURNITURE_PAUSE) return furniture + " world-gen furniture blocks are waiting to be placed";
        int chunks = loadedChunks(world);
        if (chunks >= CHUNKS_PAUSE) return chunks + " chunks are loaded (unloading is falling behind)";
        double memory = memoryUsed();
        if (memory >= MEMORY_PAUSE) return Math.round(memory * 100) + "% of memory is in use";
        return null;
    }

    private static int loadedChunks(String world) {
        var loaded = Bukkit.getWorld(world);
        return loaded == null ? 0 : loaded.getChunkCount();
    }

    /**
     * How full memory is with what's actually still in use: the long-lived heap as the last collection left it.
     * (Used-right-now counts garbage not yet collected, which routinely reaches 80% with nothing wrong.)
     */
    private static double memoryUsed() {
        for (var pool : java.lang.management.ManagementFactory.getMemoryPoolMXBeans()) {
            var afterGc = pool.getCollectionUsage();
            if (pool.getType() == java.lang.management.MemoryType.HEAP && afterGc != null && pool.getName().contains("Old")) {
                long max = pool.getUsage().getMax() > 0 ? pool.getUsage().getMax() : Runtime.getRuntime().maxMemory();
                return afterGc.getUsed() / (double) max;
            }
        }
        var runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (double) runtime.maxMemory();
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
