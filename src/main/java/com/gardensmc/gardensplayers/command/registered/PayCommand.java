package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.economy.PlayerEconomyHandler;
import com.gardensmc.gardensplayers.util.MessageUtil;
import org.bukkit.entity.Player;

/**
 * pay <player> <amount>
 */
public class PayCommand extends PlayerExecutorCommand {

    public PayCommand() {
        super("pay", 2);
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        Player targetPlayer = getPlayerOrThrow(args[0]);
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("amount");
        }
        pay(executor, amount, targetPlayer);
    }

    // the target is credited only once the sender's debit has actually gone through
    private void pay(Player executor, int amount, Player targetPlayer) {
        var economySender = new PlayerEconomyHandler(executor.getUniqueId());
        var messageSender = new SenderMessageHandler(executor);
        if (!economySender.hasEnough(amount)) {
            messageSender.sendError("You do not have enough money to make this payment");
            return;
        }
        try {
            economySender.withdraw(amount).thenAccept(taken -> {
                if (!taken) {
                    messageSender.sendError("You do not have enough money to make this payment");
                    return;
                }
                messageSender.sendMessage("Sent %s%s to %s".formatted(
                        GardensPlayers.config.getCurrencySymbol(),
                        amount,
                        MessageUtil.toPlainText(targetPlayer.displayName())
                ));
                depositIntoTarget(executor, amount, targetPlayer);
            });
        } catch (IllegalStateException | IllegalArgumentException e) {
            messageSender.sendError(e.getMessage());
        }
    }

    private void depositIntoTarget(Player executor, int amount, Player targetPlayer) {
        var economyTarget = new PlayerEconomyHandler(targetPlayer.getUniqueId());
        economyTarget
                .deposit(amount)
                .thenRun(() -> new SenderMessageHandler(targetPlayer).sendMessage("%s sent you %s%s".formatted(
                        MessageUtil.toPlainText(executor.displayName()),
                        GardensPlayers.config.getCurrencySymbol(),
                        amount
                )));
    }
}
