package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.shop.BedrockShopMenu;
import com.gardensmc.gardensplayers.menu.tile.TileMenuHandler;
import com.gardensmc.gardensplayers.util.BedrockUtil;
import org.bukkit.entity.Player;

public class ShopCommand extends PlayerExecutorCommand {

    public ShopCommand() {
        super("shop");
    }

    @Override
    public void execute(Player executor, String[] args) {
        if (BedrockUtil.isBedrock(executor)) {
            BedrockShopMenu.open(executor);
        } else {
            TileMenuHandler.openShopMenu(executor);
        }
    }

    // doesn't check its permission node: available to everyone
    @Override
    public boolean isOpenToAll() {
        return true;
    }
}
