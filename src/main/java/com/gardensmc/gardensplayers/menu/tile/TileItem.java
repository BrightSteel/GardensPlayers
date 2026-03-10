package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.menu.TileMenu;
import de.themoep.inventorygui.GuiElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Getter
public abstract class TileItem {

    private boolean isSpacer;

    public abstract ItemStack getItemStack(Player viewer);

    public abstract void onClick(GuiElement.Click click, TileMenu tileMenu);

    public abstract TileColor getTileColor(Player viewer);

    @Getter
    public enum TileColor {
        GRAY('\u013B', '\u013C', '\u013D'),
        RED('\u0140', '\u0141', '\u0142'),
        GREEN('\u0148', '\u0149', '\u014A');

        private final char[] characters;

        TileColor(char... characters) {
            this.characters = characters;
        }
    }

}
