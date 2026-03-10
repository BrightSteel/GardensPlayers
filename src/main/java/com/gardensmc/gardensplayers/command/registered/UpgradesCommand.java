package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.menu.UpgradesMenu;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpgradesCommand extends PlayerCommand {

    public UpgradesCommand() {
        super("upgrades");
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        new UpgradesMenu(player).open();
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        new UpgradesMenu(targetPlayer).open();
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Opened upgrades menu for player: " + targetPlayer.getName()));
    }
}
