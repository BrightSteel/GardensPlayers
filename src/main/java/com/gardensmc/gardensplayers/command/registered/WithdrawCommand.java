package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WithdrawCommand extends PlayerExecutorCommand {

    public WithdrawCommand() {
        super("withdraw", 1);
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("amount");
        }
        var acorns = AcornsCurrencyHandler.INSTANCE;
        var messageSender = new SenderMessageHandler(executor);
        if (!acorns.has(executor.getUniqueId(), amount)) {
            messageSender.sendError("You do not have enough money to make this payment");
        } else {
            try {
                acorns.takeCurrency(executor.getUniqueId(), amount).thenAccept(taken -> {
                    if (!taken) {
                        messageSender.sendError("You do not have enough money to make this payment");
                        return;
                    }
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                        if (!executor.isOnline()) {
                            acorns.addCurrency(executor.getUniqueId(), amount); // logged out before the acorns could be given
                            return;
                        }
                        PlayerUtil.giveItems(executor, GardensFurniture.itemRegistry.buildItem("golden_acorn"), amount);
                    });
                    messageSender.sendMessage("Withdrew %s %s".formatted(amount, GardensPlayers.config.getCurrencySymbol()));
                });
            } catch (IllegalStateException | IllegalArgumentException e) {
                messageSender.sendError(e.getMessage());
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? List.of("1", "8", "16", "32", "64") : List.of();
    }
}
