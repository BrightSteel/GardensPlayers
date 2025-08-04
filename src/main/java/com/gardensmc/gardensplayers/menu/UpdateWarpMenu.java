package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class UpdateWarpMenu extends WarpMenu {

    private final int warpId;

    public UpdateWarpMenu(Player viewer, WarpEntry warpEntry) {
        super(viewer, warpEntry.getName(), warpEntry.isPublicAccess(), warpEntry.getCategory(), "<green>Update Warp", "<font:gardens:default>\u0103\u0127</font>", warpEntry.getDescription());
        this.warpId = warpEntry.getId();
    }

    @Override
    protected void onUpdate() {
        WarpsHandler.INSTANCE.updateWarpMetadata(warpId, warpName, getCategory(), isPublic, description).thenRun(() -> viewer.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Updated warp " + warpName + "<green>!"))
        );

    }
}
