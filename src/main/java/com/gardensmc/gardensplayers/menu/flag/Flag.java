package com.gardensmc.gardensplayers.menu.flag;

import de.themoep.inventorygui.GuiElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Getter
public class Flag {
    private final String id;
    private final ItemStack item;
    private final String[] text;

    public void onClick(GuiElement.Click click) {
        Player viewer = (Player) click.getWhoClicked();
        if (click.getType() == ClickType.RIGHT) {
            viewer.chat("/unsetclaimflag " + id);
        } else {
            viewer.chat("/setclaimflag " + id);
        }
    }
}
