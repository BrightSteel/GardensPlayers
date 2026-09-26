package com.gardensmc.gardensplayers.currency;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Currency {

    String name();

    String symbol();

    int getCurrency(UUID uuid);

    default int getCurrency(Player player) {
        return getCurrency(player.getUniqueId());
    }

    default boolean has(UUID uuid, int amount) {
        return getCurrency(uuid) >= amount;
    }

    CompletableFuture<Void> addCurrency(Player player, int amount);

    CompletableFuture<Void> addCurrency(UUID player, int amount);

    CompletableFuture<Void> setCurrency(Player player, int amount);

    CompletableFuture<Void> setCurrency(UUID player, int amount);

    // takes `amount` only if the balance covers it; completes with whether it was taken
    CompletableFuture<Boolean> takeCurrency(UUID player, int amount);

    default CompletableFuture<Boolean> chargeCurrency(Player player, int amount) {
        return takeCurrency(player.getUniqueId(), amount);
    }
}
