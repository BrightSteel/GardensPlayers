package com.gardensmc.gardensplayers.rtp;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.*;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RtpHandler {

    public static final RtpHandler INSTANCE = new RtpHandler();
    private static final int MAX_TRIES = 20;
    private static final int LOCATIONS_TO_QUEUE = 5;

    private final Map<UUID, Queue<Location>> worldLocationQueue = new ConcurrentHashMap<>();

    @Nullable
    public Location getQueuedLocation(World world) {
        return worldLocationQueue
                .computeIfAbsent(world.getUID(), k -> new ConcurrentLinkedQueue<>())
                .poll();
    }

    public void queueLocations() {
        Bukkit.getWorlds().forEach(world -> tryPopulateSafeLocations(world, 0));
    }

    private void tryPopulateSafeLocations(World world, int tries) {
        if (tries > MAX_TRIES) {
            return;
        }
        var queuedLocations = worldLocationQueue.computeIfAbsent(world.getUID(), s -> new ConcurrentLinkedQueue<>());
        if (queuedLocations.size() < LOCATIONS_TO_QUEUE) {
            var location = getRandomLocation(world);
            world.getChunkAtAsync(location)
                    .thenApply(Chunk::getChunkSnapshot)
                    .thenAccept(chunkSnapshot -> {
                        var safeLocation = tryGetSafeLocation(location, chunkSnapshot);
                        if (safeLocation != null) {
                            queuedLocations.add(safeLocation);
                            GardensPlayers.plugin.getLogger().info("Queued location for world: %s (%s/%s)".formatted(world.getName(), queuedLocations.size(), LOCATIONS_TO_QUEUE));
                        }
                    }).thenRun(() -> tryPopulateSafeLocations(world, tries + 1));
            }
    }

    // run async
    @Nullable
    private Location tryGetSafeLocation(Location location, ChunkSnapshot chunkSnapshot) {
        var relX = location.getBlockX() & 15;
        var relZ = location.getBlockZ() & 15;
        var y = chunkSnapshot.getHighestBlockYAt(relX, relZ);
        if (location.getWorld().getEnvironment() == World.Environment.NETHER) {
            for (int i = 0; i < 127; i++) {
                if (isSolidBlock(chunkSnapshot.getBlockType(relX, i, relZ))) {
                    if (chunkSnapshot.getBlockType(relX, i + 1, relZ).isAir() && chunkSnapshot.getBlockType(relX, i + 2, relZ).isAir()) {
                        return new Location(location.getWorld(), location.x(), i + 1, location.z());
                    }
                }
            }
        } else if (isSolidBlock(chunkSnapshot.getBlockType(relX, y, relZ))) {
            return new Location(location.getWorld(), location.x(), y + 1, location.z());
        }
        return null;
    }

    private boolean isSolidBlock(Material material) {
        return material.isSolid() && material != Material.AIR && material != Material.LAVA && material != Material.WATER && material != Material.FIRE;
    }

    private Location getRandomLocation(World world) {
        var radius = (int) world.getWorldBorder().getSize() / 2;
        var x = getRandInt(100, radius - 500);
        var z = getRandInt(100, radius - 500);
        return new Location(world, x, 0, z);
    }

    private int getRandInt(int origin, int bound) {
        var sign = ThreadLocalRandom.current().nextBoolean() ? -1 : 1;
        return ThreadLocalRandom.current().nextInt(origin, bound) * sign;
    }
}
