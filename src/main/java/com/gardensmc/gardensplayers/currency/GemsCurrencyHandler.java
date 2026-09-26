package com.gardensmc.gardensplayers.currency;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GemsCurrencyHandler implements Currency {

    public static final GemsCurrencyHandler INSTANCE = new GemsCurrencyHandler();

    @Override
    public String name() {
        return "gems";
    }

    @Override
    public String symbol() {
        return "<white><font:gardens:default>\u0147</font></white>";
    }

    @Override
    public int getCurrency(UUID uuid) {
        return PlayerEntryCache.INSTANCE.getSync(uuid).getPoints();
    }

    // completes with true only if the player had enough gems and they were taken
    public CompletableFuture<Boolean> subtractGems(Player player, int amount) {
        return takeCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Boolean> takeCurrency(UUID player, int amount) {
        return CompletableFuture.supplyAsync(() -> Tables.playerTable.subtractPoints(player, amount))
                .whenComplete((taken, error) -> PlayerEntryCache.INSTANCE.remove(player));
    }

    @Override
    public CompletableFuture<Void> addCurrency(Player player, int amount) {
        return addCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Void> addCurrency(UUID player, int amount) {
        return CompletableFuture.runAsync(() -> Tables.playerTable.addPoints(player, amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player));
    }

    @Override
    public CompletableFuture<Void> setCurrency(Player player, int amount) {
        return setCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Void> setCurrency(UUID player, int amount) {
        return CompletableFuture
                .runAsync(() -> Tables.playerTable.setPoints(player.toString(), amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player));
    }
}
