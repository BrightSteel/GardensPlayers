package com.gardensmc.gardensplayers.menu;

import org.bukkit.entity.Player;

public class CratesMenu extends Menu {

    private static final String[] LAYOUT = new String[] {
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "bbb   ccc"
    };

    public CratesMenu(Player viewer) {
        super(LAYOUT, "\uE109", viewer);
    }

}
