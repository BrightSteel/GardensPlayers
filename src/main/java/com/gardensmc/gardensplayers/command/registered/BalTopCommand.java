package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.database.PlayerTable;
import com.gardensmc.gardensplayers.database.Tables;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BalTopCommand implements CommandExecutor {

    private static List<PlayerTable.PlayerBal> topBalances;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        var playerBals = getTopBalances();
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Top Balances:"));
        for (int i = 0; i < playerBals.size(); i++) {
            var playerBal = playerBals.get(i);
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>%s. %s: %s".formatted(i + 1, playerBal.username(), playerBal.balance()))
            );
        }
        return true;
    }

    private List<PlayerTable.PlayerBal> getTopBalances() {
        if (topBalances != null) {
            return topBalances;
        }
        try {
            topBalances = CompletableFuture.supplyAsync(Tables.playerTable::getTopBalances).get();
            // force refresh in 4 minutes
            Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> topBalances = null, 4800);
            return topBalances;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
