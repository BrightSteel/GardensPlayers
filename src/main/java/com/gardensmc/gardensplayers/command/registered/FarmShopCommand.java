package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.shop.FarmShopMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FarmShopCommand extends PlayerCommand {

    public FarmShopCommand() {
        super("farmshop");
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        new FarmShopMenu(targetPlayer).open();
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        new FarmShopMenu(player).open();
    }
}
