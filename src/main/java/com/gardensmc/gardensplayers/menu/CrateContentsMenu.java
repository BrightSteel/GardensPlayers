package com.gardensmc.gardensplayers.menu;

import org.bukkit.entity.Player;

public class CrateContentsMenu extends Menu {

    private static final String[] LAYOUT = new String[] {
            "       a ",
            "         ",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa"
    };

    public CrateContentsMenu(Player viewer) {
        super(LAYOUT, "\uE10A", viewer);
    }

}
