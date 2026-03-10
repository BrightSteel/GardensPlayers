package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.menu.DepositGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DepositCommand extends PlayerCommand {

    public DepositCommand() {
        super("deposit");
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        new DepositGui(player).open();
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        new DepositGui(targetPlayer).open();
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Opened deposit gui for player: " + targetPlayer.getName()));
    }
}
