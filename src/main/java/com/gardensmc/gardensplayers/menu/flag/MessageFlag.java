package com.gardensmc.gardensplayers.menu.flag;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import de.themoep.inventorygui.GuiElement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MessageFlag extends Flag {

    public MessageFlag(String id, ItemStack item, String[] text) {
        super(id, item, text);
    }

    @Override
    public void onClick(GuiElement.Click click) {
        var gui = click.getGui();
        gui.close();
        Player viewer = (Player) click.getWhoClicked();
        OnlinePlayerCache.INSTANCE.getOnlinePlayerData(viewer.getUniqueId()).setTextCapture(text -> {
            viewer.chat("/setclaimflag %s %s".formatted(getId(), text));
            viewer.chat("/claimflags");
        });
    }
}
