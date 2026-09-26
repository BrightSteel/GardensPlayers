package com.gardensmc.gardensplayers.quest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.block.Block;

import java.time.Duration;
import java.util.UUID;

/**
 * Blocks players placed recently, so breaking them again doesn't count towards quests. Place-and-break farming
 * happens within seconds, so remembering placements for a couple of hours (in memory) is enough.
 */
public final class PlacedBlocks {

    private record Position(UUID world, int x, int y, int z) {
        static Position of(Block block) {
            return new Position(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private static final Cache<Position, Boolean> PLACED = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))
            .maximumSize(200_000)
            .build();

    private PlacedBlocks() {
    }

    public static void markPlaced(Block block) {
        PLACED.put(Position.of(block), true);
    }

    // whether a player placed this block recently; forgets it, since the block is being broken
    public static boolean takePlaced(Block block) {
        var position = Position.of(block);
        var placed = PLACED.getIfPresent(position) != null;
        PLACED.invalidate(position);
        return placed;
    }
}
