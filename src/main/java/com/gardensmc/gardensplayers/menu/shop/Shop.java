package com.gardensmc.gardensplayers.menu.shop;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class Shop {

    /** Its file's name, without .yml: what shops/order.yml lists. */
    private final String id;
    private final String name;
    private final String furnitureId;
    /** Lines under the tab's name (MiniMessage): shop files' {@code lore:}. */
    private final List<String> lore;
    private final List<ShopItem> shopItems;

    public Shop(String fileId, YamlConfiguration shopConfiguration) {
        this.id = fileId;
        this.name = shopConfiguration.getString("name");
        this.furnitureId = shopConfiguration.getString("furniture_id");
        this.lore = List.copyOf(shopConfiguration.getStringList("lore"));
        var itemSection = shopConfiguration.getConfigurationSection("items");
        var items = new ArrayList<ShopItem>();
        if (itemSection == null) {
            GardensPlayers.plugin.getLogger().warning("Shop '" + name + "' has no items section");
        } else {
            for (var id : itemSection.getKeys(false)) {
                var section = itemSection.getConfigurationSection(id);
                if (section == null) continue;
                try {
                    items.add(new ShopItem(id, section));
                } catch (Exception e) {
                    // one typo (an unknown material or currency) shouldn't take the whole shop down
                    GardensPlayers.plugin.getLogger().severe("Skipping shop item '" + id + "' in shop '" + name + "': " + e);
                }
            }
        }
        this.shopItems = List.copyOf(items);
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
