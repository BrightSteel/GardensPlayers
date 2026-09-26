package com.gardensmc.gardensplayers.player;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import de.tr7zw.nbtapi.NBT;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
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

    // completes with true only if the balance covered it and the money was taken
    public CompletableFuture<Boolean> subtractMoney(int amount) {
        return CompletableFuture
                .supplyAsync(() -> Tables.playerTable.subtractMoney(uuid, amount))
                .whenComplete((taken, error) -> {
                    if (Boolean.TRUE.equals(taken)) {
                        PlayerEntryCache.INSTANCE.updateIfLoaded(uuid, entry -> entry.adjustMoney(-amount));
                    } else {
                        refreshCache(); // the cached balance was wrong - reload it
                    }
                });
    }

    public CompletableFuture<Void> addMoney(int amount) {
        return CompletableFuture
                .runAsync(() -> Tables.playerTable.addMoney(uuid, amount))
                .whenComplete((ignored, error) -> {
                    if (error == null) {
                        PlayerEntryCache.INSTANCE.updateIfLoaded(uuid, entry -> entry.adjustMoney(amount));
                    } else {
                        refreshCache();
                    }
                });
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

    public CompletableFuture<Void> updateWardrobe(CosmeticSlot slot, String nbt) {
        var player = Objects.requireNonNull(Bukkit.getPlayer(uuid));
        return CompletableFuture.runAsync(() -> {
            Tables.wardrobeTable.createWardrobeEntryIfNotExists(player);
            if (slot == CosmeticSlot.HELMET) {
                Tables.wardrobeTable.setHelmet(player, nbt);
            } else if (slot == CosmeticSlot.BACKPACK) {
                Tables.wardrobeTable.setBackpack(player, nbt);
            } else if (slot == CosmeticSlot.OFFHAND) {
                Tables.wardrobeTable.setOffhand(player, nbt);
            } else if (slot == CosmeticSlot.BALLOON) {
                Tables.wardrobeTable.setBalloon(player, nbt);
            }
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
