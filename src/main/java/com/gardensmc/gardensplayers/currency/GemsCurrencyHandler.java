package com.gardensmc.gardensplayers.currency;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

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
    public int getCurrency(Player player) {
        return PlayerEntryCache.INSTANCE.getSync(player.getUniqueId()).getPoints();
    }

    public CompletableFuture<Void> subtractGems(Player player, int amount) {
        return CompletableFuture.runAsync(() -> Tables.playerTable.subtractPoints(player.getUniqueId(), amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player.getUniqueId()));
    }

    @Override
    public CompletableFuture<Void> addCurrency(Player player, int amount) {
        return CompletableFuture.runAsync(() -> Tables.playerTable.addPoints(player.getUniqueId(), amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player.getUniqueId()));
    }

    @Override
    public CompletableFuture<Void> setCurrency(Player player, int amount) {
        return CompletableFuture
                .runAsync(() -> Tables.playerTable.setPoints(player.getUniqueId().toString(), amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player.getUniqueId()));
    }
}
