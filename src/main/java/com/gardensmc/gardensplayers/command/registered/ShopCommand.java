package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.shop.BedrockShopMenu;
import com.gardensmc.gardensplayers.menu.tile.TileMenuHandler;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class ShopCommand extends PlayerExecutorCommand {

    public ShopCommand() {
        super("shop");
    }

    @Override
    public void execute(Player executor, String[] args) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(executor.getUniqueId())) {
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
