package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import org.bukkit.entity.Player;

public class SkinsTileMenu extends TileMenu {

    private static final char BG = '1';

    public SkinsTileMenu(Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber) {
        super(viewer, pages, page, selectedCategory, guiPageNumber, false, BG);
    }

    public SkinsTileMenu(Player viewer, CategoryPages pages, int page) {
        super(viewer, pages, page, false, BG);
    }

    public SkinsTileMenu(Player viewer, CategoryPages pages) {
        super(viewer, pages, false, BG);
    }
}
