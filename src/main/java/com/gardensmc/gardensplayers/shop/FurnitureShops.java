package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.util.ConfigUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FurnitureShops {

    public static final FurnitureShops INSTANCE = new FurnitureShops();

    public FurnitureShop getFurnitureShop(String shop, CustomItem customItem) {
        var shopSection = ConfigUtil.furnitureShopsYaml.getConfigurationSection(shop);
        if (shopSection != null) {
            var itemSection = shopSection.getConfigurationSection(customItem.getIdentifier());
            if (itemSection != null) {
                var price = itemSection.getInt("price");
                return new FurnitureShop(customItem, price);
            }
        }
        return null;
    }
}
