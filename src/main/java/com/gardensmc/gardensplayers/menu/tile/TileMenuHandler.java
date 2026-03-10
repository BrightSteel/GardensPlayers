package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TileMenuHandler {

    private static CategoryPages shopPages;
    private static CategoryPages wardrobePages;
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

    public static void constructWardrobePages() {
        var cosmeticsBySlot = HMCCosmeticsAPI.getAllCosmetics().stream().collect(Collectors.groupingBy(Cosmetic::getSlot, Collectors.mapping(
                Function.identity(), Collectors.toList()
        )));
        var categories = COSMETIC_CATEGORIES.stream().map(cosmeticSlot -> new TileCategory(
                cosmeticSlot.getName(),
                cosmeticSlot.getName(),
                "icon_" + cosmeticSlot.getName(),
                cosmeticsBySlot.get(cosmeticSlot) == null ? Collections.emptyList() : cosmeticsBySlot.get(cosmeticSlot).stream().map(CosmeticTileItem::new).toList()
        )).toList();
        wardrobePages = new CategoryPages(categories);
    }

    public static void openShopMenu(Player viewer) {
        new TileMenu(viewer, TileMenuHandler.shopPages, true, '\u0138').open();
    }

    public static void openCosmeticsMenu(Player viewer) {
        new TileMenu(viewer, TileMenuHandler.wardrobePages, false, '\u014B').open();
    }
}
