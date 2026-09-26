package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.economy.PlayerEconomyHandler;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
        var economySender = new PlayerEconomyHandler(executor.getUniqueId());
        var messageSender = new SenderMessageHandler(executor);
        if (!economySender.hasEnough(amount)) {
            messageSender.sendError("You do not have enough money to make this payment");
        } else {
            try {
                economySender.withdraw(amount).thenAccept(taken -> {
                    if (!taken) {
                        messageSender.sendError("You do not have enough money to make this payment");
                        return;
                    }
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                        if (!executor.isOnline()) {
                            economySender.deposit(amount); // logged out before the acorns could be given
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
}
