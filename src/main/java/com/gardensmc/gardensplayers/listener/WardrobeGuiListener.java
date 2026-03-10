package com.gardensmc.gardensplayers.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.menu.WardrobeGui;
import com.gardensmc.gardensplayers.menu.wardrobe.ArmorWardrobeSlot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class WardrobeGuiListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        if (clickedInv != null && clickedInv.getHolder() instanceof WardrobeGui wardrobeGui) {
            var item = e.getCursor();
            var wardrobeSlot = WardrobeGui.WARDROBE_SLOTS.get(e.getSlot());
            if (wardrobeSlot == null) {
                e.setCancelled(true);
            } else if (item.getType() != Material.AIR && !wardrobeSlot.isValidItem(item)) {
                e.setCancelled(true);
            } else {
                Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> wardrobeGui.updateWardrobeSlot(e.getSlot(), wardrobeSlot), 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && e.getInventory().getHolder() instanceof WardrobeGui) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onShiftInto(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof PlayerInventory) {
            if (e.getView().getTopInventory().getHolder() instanceof WardrobeGui wardrobeGui) {
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    e.setCancelled(true);
                    var item = e.getClickedInventory().getItem(e.getSlot());
                    var wardrobeEntry = WardrobeGui.WARDROBE_SLOTS.entrySet()
                            .stream()
                            .filter(entry -> isNullOrAir(wardrobeGui.getInventory().getItem(entry.getKey())) && entry.getValue().isValidItem(item))
                            .findFirst()
                            .orElse(null);
                    if (wardrobeEntry != null) {
                        wardrobeGui.getInventory().setItem(wardrobeEntry.getKey(), item);
                        e.getClickedInventory().setItem(e.getSlot(), null);
                        Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> wardrobeGui.updateWardrobeSlot(wardrobeEntry.getKey(), wardrobeEntry.getValue()), 1);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        // update open wardrobes
        if (e.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof WardrobeGui wardrobeGui) {
            WardrobeGui.WARDROBE_SLOTS.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof ArmorWardrobeSlot && ((ArmorWardrobeSlot) entry.getValue()).getEquipmentSlot() == e.getSlot())
                    .findFirst()
                    .ifPresent(entry -> Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> wardrobeGui.updateWardrobeSlot(entry.getKey(), entry.getValue()), 1));
        }
    }

    private boolean isNullOrAir(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR;
    }
}
