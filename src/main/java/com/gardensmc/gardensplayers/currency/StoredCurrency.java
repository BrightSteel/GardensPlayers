package com.gardensmc.gardensplayers.currency;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A currency kept in a column of the player table. The database has the final say (a debit is one conditional
 * update); the cached entry is adjusted in place once a change succeeds, and reloaded when the cache turns out to
 * be wrong. Every change to a balance goes through here - the Vault provider, commands, shops and rewards alike.
 */
public abstract class StoredCurrency implements Currency {

    protected abstract int read(PlayerEntry entry);

    protected abstract void adjustCached(PlayerEntry entry, int delta);

    protected abstract void addStored(UUID player, int amount);

    // takes `amount` only if the stored balance covers it
    protected abstract boolean takeStored(UUID player, int amount);

    protected abstract void setStored(UUID player, int amount);

    // 0 for a player with no entry. Blocks while an offline player's entry loads
    @Override
    public int getCurrency(UUID uuid) {
        var entry = PlayerEntryCache.INSTANCE.getSync(uuid);
        return entry == null ? 0 : read(entry);
    }

    @Override
    public CompletableFuture<Void> addCurrency(Player player, int amount) {
        return addCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Void> addCurrency(UUID player, int amount) {
        validatePositive(amount);
        return CompletableFuture.runAsync(() -> addStored(player, amount))
                .whenComplete((ignored, error) -> {
                    if (error == null) {
                        PlayerEntryCache.INSTANCE.updateIfLoaded(player, entry -> adjustCached(entry, amount));
                    } else {
                        PlayerEntryCache.INSTANCE.remove(player);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> takeCurrency(UUID player, int amount) {
        validatePositive(amount);
        return CompletableFuture.supplyAsync(() -> takeStored(player, amount))
                .whenComplete((taken, error) -> {
                    if (Boolean.TRUE.equals(taken)) {
                        PlayerEntryCache.INSTANCE.updateIfLoaded(player, entry -> adjustCached(entry, -amount));
                    } else {
                        PlayerEntryCache.INSTANCE.remove(player); // the cached balance was wrong - reload it
                    }
                });
    }

    @Override
    public CompletableFuture<Void> setCurrency(Player player, int amount) {
        return setCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Void> setCurrency(UUID player, int amount) {
        return CompletableFuture.runAsync(() -> setStored(player, amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player));
    }

    private static void validatePositive(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be positive!");
        }
    }
}
