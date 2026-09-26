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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class BalTopCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGES = 5;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static List<PlayerTable.PlayerBal> topBalances;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        var cached = topBalances;
        if (cached != null) {
            sendPage(sender, cached, args);
            return true;
        }
        // load off the main thread, then answer from it
        CompletableFuture.supplyAsync(Tables.playerTable::getTopBalances).whenComplete((balances, error) ->
                Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                    if (error != null) {
                        GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to load top balances", error);
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>Couldn't load the top balances, try again later."));
                        return;
                    }
                    if (topBalances == null) {
                        topBalances = balances;
                        // force refresh in 4 minutes
                        Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> topBalances = null, 4800);
                    }
                    sendPage(sender, balances, args);
                }));
        return true;
    }

    private void sendPage(CommandSender sender, List<PlayerTable.PlayerBal> balances, String[] args) {
        if (balances.isEmpty()) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<gray>There are no balances to show yet."));
            return;
        }

        int totalPages = Math.min(MAX_PAGES, (int) Math.ceil(balances.size() / (double) PAGE_SIZE));
        int page = Math.min(parsePage(args), totalPages);
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, balances.size());
        var symbol = GardensPlayers.config.getCurrencySymbol();

        sender.sendMessage(MINI_MESSAGE.deserialize(
                "<gradient:#FFE259:#FFA751><bold>★ Top Balances ★</bold></gradient> <gray>(page <white>%s</white>/<white>%s</white>)"
                        .formatted(page, totalPages)));
        for (int i = from; i < to; i++) {
            int rank = i + 1;
            var balance = balances.get(i);
            sender.sendMessage(MINI_MESSAGE.deserialize("%s#%s <gray>%s%s <dark_gray>» <gold>%s%s"
                    .formatted(rankColor(rank), rank, nameColor(rank), balance.username(), symbol,
                            NUMBER_FORMAT.format(balance.balance()))));
        }
        if (page < totalPages) {
            sender.sendMessage(MINI_MESSAGE.deserialize(
                    "<dark_gray>» <gray>Use <yellow>/baltop %s</yellow> <gray>for the next page".formatted(page + 1)));
        }
    }

    private int parsePage(String[] args) {
        if (args.length == 0) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(MAX_PAGES, Integer.parseInt(args[0])));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<#CD7F32>";
            default -> "<dark_gray>";
        };
    }

    private String nameColor(int rank) {
        return rank <= 3 ? rankColor(rank) : "<white>";
    }
}
