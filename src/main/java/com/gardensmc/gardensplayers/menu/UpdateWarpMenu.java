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
        WarpsHandler.INSTANCE.updateWarpMetadata(warpId, warpName, getCategory(), isPublic, description).whenComplete((ignored, error) -> {
            var cause = error != null && error.getCause() != null ? error.getCause() : error;
            viewer.sendMessage(MiniMessage.miniMessage().deserialize(error == null
                    ? "<green>Updated warp " + WarpsHandler.cleanText(warpName) + "<green>!"
                    : "<red>" + cause.getMessage()));
        });
    }
}
