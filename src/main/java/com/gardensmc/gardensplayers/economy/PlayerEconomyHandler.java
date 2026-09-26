package com.gardensmc.gardensplayers.economy;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class PlayerEconomyHandler {

    private final UUID uuid;

    public boolean hasEnough(int amount) {
        var playerEntry = playerEntry();
        if (playerEntry == null) {
            return false;
        }
        return playerEntry.getMoney() >= amount;
    }

    public int getBalance() {
        var playerEntry = playerEntry();
        if (playerEntry == null) {
            return 0;
        }
        return playerEntry.getMoney();
    }

    /**
     * Throws right away when the (cached) balance is clearly too low, for a friendly message. The database has
     * the final say: the future completes with false if the balance no longer covers it (e.g. a second
     * withdrawal in the same tick). Only act on true.
     */
    public CompletableFuture<Boolean> withdraw(int amount) throws IllegalArgumentException, IllegalStateException {
        validateAmountPositive(amount);
        var playerEntry = getPlayerEntryOrThrow();
        if ((playerEntry.getMoney() - amount) < 0) {
            throw new IllegalArgumentException("Cannot overdraw balance!");
        }
        return new PlayerEntryHandler(uuid).subtractMoney(amount);
    }

    public CompletableFuture<Void> deposit(int amount) throws IllegalArgumentException, IllegalStateException {
        validateAmountPositive(amount);
        return new PlayerEntryHandler(uuid).addMoney(amount);
    }

    private void validateAmountPositive(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Value cannot be negative or zero!");
        }
    }

    private PlayerEntry getPlayerEntryOrThrow() throws IllegalStateException {
        var playerEntry = playerEntry();
        if (playerEntry == null) {
            throw new IllegalStateException("Cannot find user!");
        }
        return playerEntry;
    }

    @Nullable
    private PlayerEntry playerEntry() {
        return PlayerEntryCache.INSTANCE.getSync(uuid);
    }
}
