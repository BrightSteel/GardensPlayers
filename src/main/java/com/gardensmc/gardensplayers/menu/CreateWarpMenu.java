package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class CreateWarpMenu extends WarpMenu {

    public CreateWarpMenu(Player viewer, String warpName) {
        super(viewer, warpName, true, WarpEntry.Category.MISC, "<green>Create Warp", "<font:gardens:default>\u0103\u0126</font>", null);
    }

    @Override
    public void onUpdate() {
        var result = WarpsHandler.INSTANCE.createWarp(
                viewer,
                warpName,
                viewer.getLocation(),
                getCategory(),
                isPublic,
                description
        );
        if (result.isSuccess()) {
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Created warp " + warpName + "<green>!"));
        } else {
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + result.message()));
        }
    }
}
