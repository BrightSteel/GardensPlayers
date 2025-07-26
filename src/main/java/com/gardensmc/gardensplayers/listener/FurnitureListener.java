package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.events.CustomBlockInteractEvent;
import com.gardensmc.gardensfurniture.events.FurnitureInteractEvent;
import com.gardensmc.gardensplayers.shop.FurnitureShops;
import com.gardensmc.gardensplayers.shop.ShopQuantityMenu;
import com.gardensmc.gardensplayers.shop.TransactionMode;
import com.gardensmc.gardensplayers.util.WorldGuardUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

public class FurnitureListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onFurnitureInteract(FurnitureInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND) {
            openFurnitureShopIfApplicable(e.getPlayer(), e.getFurniture().customItem());
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCustomBlockInteract(CustomBlockInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND) {
            openFurnitureShopIfApplicable(e.getPlayer(), e.getCustomItem());
            e.setCancelled(true);
        }
    }

    private void openFurnitureShopIfApplicable(Player player, CustomItem customItem) {
        var regionSet = WorldGuardUtil.getRegionsAt(player.getLocation());
        if (regionSet != null) {
            var shop = regionSet.getRegions().stream().filter(rg -> rg.getId().contains("shop")).findFirst().orElse(null);
            if (shop != null) {
                var furnitureShop = FurnitureShops.INSTANCE.getFurnitureShop(shop.getId(), customItem);
                if (furnitureShop != null) {
                    new ShopQuantityMenu(
                            player,
                            customItem.getDisplayRawFormatted(),
                            GardensFurniture.itemRegistry.buildItem(customItem),
                            furnitureShop.price(),
                            TransactionMode.BUY,
                            null
                    ).open();
                }
            }
        }
    }
}
