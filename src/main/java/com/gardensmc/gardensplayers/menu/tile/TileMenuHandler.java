package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.menu.TileMenu;
import org.bukkit.entity.Player;

import java.util.Comparator;

public class TileMenuHandler {

    private static CategoryPages shopPages;

    public static void constructShopPages() {
        var tileCategories = GardensPlayers.shops.stream().map(shop -> new TileCategory(
                shop.getName(),
                shop.getName(),
                shop.getFurnitureId(),
                shop.getShopItems().stream().map(ShopTileItem::new).toList()
            ))
            .sorted(Comparator.comparing(TileCategory::getId))
            .toList();
        shopPages = new CategoryPages(tileCategories);
    }

    public static void openShopMenu(Player viewer) {
        TileMenu.builder(viewer, TileMenuHandler.shopPages, '\u0138').showBalances(true).open();
    }

}
