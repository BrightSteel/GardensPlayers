package com.gardensmc.gardensplayers.currency;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.economy.PlayerEconomyHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AcornsCurrencyHandler implements Currency {

    public static final AcornsCurrencyHandler INSTANCE = new AcornsCurrencyHandler();

    @Override
    public String name() {
        return "acorns";
    }

    @Override
    public String symbol() {
        return "<white><font:gardens:default>\u0120</font></white>";
    }

    @Override
    public int getCurrency(UUID uuid) {
        return new PlayerEconomyHandler(uuid).getBalance();
    }

    @Override
    public CompletableFuture<Void> addCurrency(Player player, int amount) {
        return addCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Void> addCurrency(UUID player, int amount) {
        return new PlayerEconomyHandler(player).deposit(amount);
    }

    @Override
    public CompletableFuture<Void> setCurrency(Player player, int amount) {
        return setCurrency(player.getUniqueId(), amount);
    }

    @Override
    public CompletableFuture<Void> setCurrency(UUID player, int amount) {
        return CompletableFuture
                .runAsync(() -> Tables.playerTable.setMoney(player.toString(), amount))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(player));
    }
}
