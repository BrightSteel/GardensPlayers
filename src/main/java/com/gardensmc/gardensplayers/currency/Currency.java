package com.gardensmc.gardensplayers.currency;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface Currency {

    String name();

    String symbol();

    int getCurrency(Player player);

    CompletableFuture<Void> addCurrency(Player player, int amount);

    CompletableFuture<Void> setCurrency(Player player, int amount);

    default CompletableFuture<Void> chargeCurrency(Player player, int amount) {
        var bal = getCurrency(player);
        return setCurrency(player, bal - amount);
    }
}
