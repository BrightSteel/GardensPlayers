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
            if (openFurnitureShopIfApplicable(e.getPlayer(), e.getFurniture().customItem())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCustomBlockInteract(CustomBlockInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND) {
            if (openFurnitureShopIfApplicable(e.getPlayer(), e.getCustomItem())) {
                e.setCancelled(true);
            }
        }
    }

    /** The tick each player last had a shop opened by a click, so the fallback below doesn't open it again. */
    private final java.util.Map<java.util.UUID, Integer> openedOnTick = new java.util.HashMap<>();

    /**
     * Shop furniture that's a block (a planter box on a note block, barrier furniture) in a region where protection
     * denies interacting (WorldGuard): the click is cancelled before GardensFurniture sees it, so its event never comes.
     * Opening the shop menu is still let through; the click itself stays cancelled, changing nothing.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeniedBlockClick(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        if (e.useInteractedBlock() != org.bukkit.event.Event.Result.DENY) return;
        var player = e.getPlayer();
        if (openedOnTick.getOrDefault(player.getUniqueId(), -1) == org.bukkit.Bukkit.getCurrentTick()) return;
        var customItem = com.gardensmc.gardensfurniture.custom.CustomBlockManager.getCustomItemFromBlock(e.getClickedBlock());
        if (customItem != null) openFurnitureShopIfApplicable(player, customItem);
    }

    private boolean openFurnitureShopIfApplicable(Player player, CustomItem customItem) {
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
                    openedOnTick.put(player.getUniqueId(), org.bukkit.Bukkit.getCurrentTick());
                    return true;
                }
            }
        }
        return false;
    }
}
