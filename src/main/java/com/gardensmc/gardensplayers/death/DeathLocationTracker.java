package com.gardensmc.gardensplayers.death;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers each player's most recent death location in memory so {@code /deathback} can teleport them there.
 * Locations are not persisted — they're forgotten on server restart, which is fine for a "return to where you
 * just died" convenience.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeathLocationTracker {

    public static final DeathLocationTracker INSTANCE = new DeathLocationTracker();

    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public void set(UUID uuid, Location location) {
        deathLocations.put(uuid, location);
    }

    @Nullable
    public Location get(UUID uuid) {
        return deathLocations.get(uuid);
    }

    public void clear(UUID uuid) {
        deathLocations.remove(uuid);
    }
}
