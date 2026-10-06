package com.gardensmc.gardensplayers.player;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Anything that makes a change and requires refresh of cache should pass through here !
 */
@AllArgsConstructor
public class PlayerEntryHandler {

    private final UUID uuid;

    public static boolean playerExists(UUID uuid) {
        var promise = CompletableFuture.supplyAsync(() -> Tables.playerTable.getPlayerEntry(uuid));
        try {
            return promise.get() != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setNickname(@Nullable String nickname) {
        CompletableFuture
                .runAsync(() -> Tables.playerTable.setNickname(uuid.toString(), nickname))
                .thenRun(this::refreshCache);
    }

    public void updateLastLocation() {
        var playerEntry = getOrThrow();
        var player = Objects.requireNonNull(Bukkit.getPlayer(uuid));
        CompletableFuture.runAsync(() -> {
            if (playerEntry.getLastLocation() == null) {
                Tables.lastLocationTable.createLastLocationEntry(player);
            } else {
                Tables.lastLocationTable.setLastLocationEntry(player);
            }
            // update seen time as well while we're at it
            Tables.playerTable.setSeen(player, LocalDateTime.now(Clock.systemUTC()));
        }).thenRun(this::refreshCache);
    }


    private PlayerEntry getOrThrow() {
        try {
            return PlayerEntryCache.INSTANCE.get(uuid).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void refreshCache() {
        PlayerEntryCache.INSTANCE.remove(uuid);
    }
}
