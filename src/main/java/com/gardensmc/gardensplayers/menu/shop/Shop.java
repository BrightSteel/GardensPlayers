package com.gardensmc.gardensplayers.menu.shop;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Objects;

@Getter
public class Shop {

    private final String name;
    private final String furnitureId;
    private final List<ShopItem> shopItems;

    public Shop(YamlConfiguration shopConfiguration) {
        this.name = shopConfiguration.getString("name");
        this.furnitureId = shopConfiguration.getString("furniture_id");
        var itemSection = shopConfiguration.getConfigurationSection("items");
        this.shopItems = Objects.requireNonNull(itemSection).getKeys(false)
                .stream()
                .map(id -> Pair.of(id, itemSection.getConfigurationSection(id)))
                .filter(pair -> pair.getRight() != null)
                .map(pair -> new ShopItem(pair.getLeft(), pair.getRight()))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shop shop)) return false;
        return Objects.equals(name, shop.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
