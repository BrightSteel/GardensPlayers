package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.backpack.BackpackHandler;
import com.gardensmc.gardensplayers.menu.BackpackGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BackpackGuiListener implements Listener {

    @EventHandler
    public void onLeaveGame(PlayerQuitEvent e) {
        // sometimes it can glitch and backpack is still left in map forever - at least let leaving clear it
        BackpackHandler.clearBackpackFromMap(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        if (clickedInv != null && clickedInv.getHolder() instanceof BackpackGui backpackGui) {
            if (e.getSlot() < backpackGui.getFillerSlots()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && e.getInventory().getHolder() instanceof BackpackGui backpackGui) {
            if (e.getRawSlots().stream().anyMatch(slot -> slot < backpackGui.getFillerSlots())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemIntoBackpack(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof BackpackGui)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!BackpackHandler.isAllowedInBackpack(getItemEnteringBackpack(e, player))) {
            e.setCancelled(true);
        }
    }

    // Every click that can move an item into the backpack gui, and the item it would move.
    private ItemStack getItemEnteringBackpack(InventoryClickEvent e, Player player) {
        var clickedInv = e.getClickedInventory();
        if (clickedInv == null) {
            return null;
        }
        if (clickedInv.getHolder() instanceof BackpackGui) {
            return switch (e.getClick()) {
                case NUMBER_KEY -> player.getInventory().getItem(e.getHotbarButton());
                case SWAP_OFFHAND -> player.getInventory().getItemInOffHand();
                default -> e.getCursor(); // left/right/middle placement
            };
        }
        // shifting from player inv -> backpack
        return e.isShiftClick() ? e.getCurrentItem() : null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDragItemsIntoBackpack(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BackpackGui) {
            if (!e.getNewItems().values().stream().allMatch(BackpackHandler::isAllowedInBackpack)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof BackpackGui backpackGui) {
            BackpackHandler.onClose((Player) e.getPlayer(), backpackGui, backpackGui.getBackpackItem());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveBackpackInInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        var playerInventory = player.getInventory();
        if (e.getClickedInventory() instanceof PlayerInventory) {
            var item = e.getClickedInventory().getItem(e.getSlot());
            if (BackpackHandler.isOpenBackpack(playerInventory, item)) {
                e.setCancelled(true);
                return;
            }
        }
        // hotbar keys and offhand swaps move the hotbar/offhand item without clicking it directly
        var swapped = switch (e.getClick()) {
            case NUMBER_KEY -> playerInventory.getItem(e.getHotbarButton());
            case SWAP_OFFHAND -> playerInventory.getItemInOffHand();
            default -> null;
        };
        if (BackpackHandler.isOpenBackpack(playerInventory, swapped)) {
            e.setCancelled(true);
        }
    }

}
