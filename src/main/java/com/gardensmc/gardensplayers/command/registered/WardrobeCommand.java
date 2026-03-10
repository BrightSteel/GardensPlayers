package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.WardrobeGui;
import com.gardensmc.gardensplayers.menu.tile.TileMenuHandler;
import org.bukkit.entity.Player;

public class WardrobeCommand extends PlayerExecutorCommand {

    public WardrobeCommand() {
        super("wardrobe");
    }

    @Override
    public void execute(Player executor, String[] args) {
        new WardrobeGui(executor).open();
//        TileMenuHandler.openCosmeticsMenu(executor);
    }
}
