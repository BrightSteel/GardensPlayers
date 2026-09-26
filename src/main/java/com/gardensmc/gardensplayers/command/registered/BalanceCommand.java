package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.exception.InvalidUserException;
import com.gardensmc.gardensplayers.command.exception.NoPermissionException;
import com.gardensmc.gardensplayers.command.exception.PlayerNotFoundException;
import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.economy.PlayerEconomyHandler;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand extends PlayerCommand {

    public BalanceCommand() {
        super("balance");
    }

    @Override
    public void execute(CommandSender executor, String[] args) {
        if (args.length == 0) {
            if (executor instanceof Player player) {
                executeOnSelf(player, args);
            } else {
                throw new InvalidUserException();
            }
            return;
        }

        String username = args[0];
        if (executor instanceof Player player && displayName(player).equalsIgnoreCase(username)) {
            executeOnSelf(player, args);
            return;
        }

        // Online target — reuse the existing path.
        var onlineTarget = PlayerUtil.findOnline(username);
        if (onlineTarget != null) {
            executeOnPlayer(executor, onlineTarget, args);
            return;
        }

        // Offline target — resolve the UUID from the player table.
        if (!executor.hasPermission(permissionNodeOthers)) {
            throw new NoPermissionException(permissionNodeOthers);
        }
        runAsync(executor, () -> {
            var targetUUID = Tables.playerTable.getUUID(username);
            if (targetUUID == null) {
                throw new PlayerNotFoundException(username);
            }
            return new PlayerEconomyHandler(targetUUID).getBalance();
        }, balance -> executor.sendMessage(MessageUtil.parseIntoComponent(
                "<green>%s's balance is %s%s".formatted(
                        username,
                        GardensPlayers.config.getCurrencySymbol(),
                        balance
                )
        )));
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
                "<green>%s's balance is %s%s".formatted(
                        MessageUtil.toPlainText(targetPlayer.displayName()),
                        GardensPlayers.config.getCurrencySymbol(),
                        balance
                )
        ));
    }
}
