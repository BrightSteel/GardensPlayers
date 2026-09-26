package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Set;

public class TileMenuHandler {

    private static CategoryPages shopPages;
    private static final Set<CosmeticSlot> COSMETIC_CATEGORIES = Set.of(
            CosmeticSlot.HELMET,
            CosmeticSlot.BACKPACK,
            CosmeticSlot.OFFHAND,
            CosmeticSlot.BALLOON
    );

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
