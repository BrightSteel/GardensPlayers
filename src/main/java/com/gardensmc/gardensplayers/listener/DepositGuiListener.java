package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.menu.DepositGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.PlayerInventory;

public class DepositGuiListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        if (clickedInv != null && clickedInv.getHolder() instanceof DepositGui depositGui) {
            if (!DepositGui.ACORN_SLOTS.contains(e.getSlot())) {
                e.setCancelled(true);
            } else if (!depositGui.isValidItem(e.getCursor())) {
                e.setCancelled(true);
            } else {
                Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, depositGui::depositItems, 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && e.getInventory().getHolder() instanceof DepositGui depositGui) {
            if (!DepositGui.ACORN_SLOTS.containsAll(e.getRawSlots())) {
                e.setCancelled(true);
            } else if (!depositGui.isValidItem(e.getOldCursor())) {
                e.setCancelled(true);
            } else {
                Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, depositGui::depositItems, 1);
            }
        }
    }

    @EventHandler
    public void onShiftIntoBackpack(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof PlayerInventory) {
            if (e.getView().getTopInventory().getHolder() instanceof DepositGui depositGui) {
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    var item = e.getClickedInventory().getItem(e.getSlot());
                    if (!depositGui.isValidItem(item)) {
                        e.setCancelled(true);
                    } else {
                        Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, depositGui::depositItems, 1);
                    }
                }
            }
        }
    }
}
