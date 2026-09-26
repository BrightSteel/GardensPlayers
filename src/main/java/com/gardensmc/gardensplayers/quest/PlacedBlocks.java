package com.gardensmc.gardensplayers.quest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

/**
 * Blocks players placed recently, and by whom, so breaking them again doesn't count towards mining quests and
 * takes back the placer's place-quest progress. Place-and-break farming
 * happens within seconds, so remembering placements for a couple of hours (in memory) is enough.
 */
public final class PlacedBlocks {

    private record Position(UUID world, int x, int y, int z) {
        static Position of(Block block) {
            return new Position(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private static final Cache<Position, UUID> PLACED = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))
            .maximumSize(200_000)
            .build();

    private PlacedBlocks() {
    }

    public static void markPlaced(Block block, UUID placer) {
        PLACED.put(Position.of(block), placer);
    }

    // who placed this block recently, or null; forgets it, since the block is being broken
    @Nullable
    public static UUID takePlacer(Block block) {
        return PLACED.asMap().remove(Position.of(block));
    }
}
