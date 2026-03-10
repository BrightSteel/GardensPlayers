package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.tile.TileMenuHandler;
import org.bukkit.entity.Player;

public class ShopCommand extends PlayerExecutorCommand {

    public ShopCommand() {
        super("shop");
    }

    @Override
    public void execute(Player executor, String[] args) {
        TileMenuHandler.openShopMenu(executor);
    }
}
