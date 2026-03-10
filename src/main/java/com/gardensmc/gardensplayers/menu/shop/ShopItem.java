package com.gardensmc.gardensplayers.menu.shop;

import com.gardensmc.gardensfurniture.GardensFurniture;
import emanondev.itemedit.ItemEdit;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

@Getter
public class ShopItem {

    private final String id;
    private final Material material;
    private final String furnitureId;
    private final String serverItem;
    private final int buyPrice;
    private final int sellPrice;
    private final ShopCurrency currency;
    private final int sellLimit;

    public ShopItem(String id, ConfigurationSection shopItemSection) {
        this.id = id;
        var materialStr = shopItemSection.getString("material");
        this.material = materialStr == null ? null : Material.valueOf(materialStr.toUpperCase());
        this.furnitureId = shopItemSection.getString("furniture_id");
        this.serverItem = shopItemSection.getString("server_item");
        this.buyPrice = shopItemSection.getInt("buy_price", -1);
        this.sellPrice = shopItemSection.getInt("sell_price", -1);
        var currencyStr = shopItemSection.getString("currency");
        this.currency = currencyStr == null ? ShopCurrency.VAULT : ShopCurrency.valueOf(currencyStr.toUpperCase());
        this.sellLimit = shopItemSection.getInt("sell_limit", -1);
    }

    @Nullable
    public ItemStack getItemStack() {
        if (material != null) {
            return new ItemStack(material);
        }
        if (furnitureId != null) {
            return GardensFurniture.itemRegistry.buildItem(furnitureId.toLowerCase());
        }
        if (serverItem != null) {
            return ItemEdit.get().getServerStorage().getItem(serverItem);
        }
        return null;
    }

    public boolean hasSellLimit() {
        return sellLimit > -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopItem shopItem)) return false;
        return Objects.equals(id, shopItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
