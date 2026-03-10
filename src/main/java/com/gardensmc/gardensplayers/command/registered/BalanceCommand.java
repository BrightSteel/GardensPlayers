package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.economy.PlayerEconomyHandler;
import com.gardensmc.gardensplayers.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand extends PlayerCommand {

    public BalanceCommand() {
        super("balance");
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        var balance = new PlayerEconomyHandler(player.getUniqueId()).getBalance();
        player.sendMessage(MessageUtil.parseIntoComponent(
                "<green>Your balance is %s%s".formatted(GardensPlayers.config.getCurrencySymbol(), balance)
        ));
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        var balance = new PlayerEconomyHandler(targetPlayer.getUniqueId()).getBalance();
        executor.sendMessage(MessageUtil.parseIntoComponent(
                "<green>%s's balance is %s\uE124%s".formatted(
                        MessageUtil.toPlainText(targetPlayer.displayName()),
                        GardensPlayers.config.getCurrencySymbol(),
                        balance
                )
        ));
    }
}
