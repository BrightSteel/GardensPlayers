package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.wardrobe.WardrobeBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public class BackpackListener implements Listener {

    private static final int MAX_ROWS = 6;

    /**
     * An Expansion Rune in an anvil gives a backpack (EnderWardrobe's storage) one more row, up to a double chest.
     */
    @EventHandler
    public void onSlotRuneBackpack(PrepareAnvilEvent e) {
        var backpack = e.getInventory().getFirstItem();
        var rune = e.getInventory().getSecondItem();
        if (backpack == null || rune == null) {
            return;
        }
        var runeItem = GardensFurniture.itemRegistry.getCustomItem(rune);
        if (runeItem == null || !runeItem.getIdentifier().equals("expansion_rune")) {
            return;
        }
        int rows = WardrobeBridge.backpackRows(backpack);
        if (rows == 0 || rows >= MAX_ROWS) {
            return;
        }
        var result = WardrobeBridge.withBackpackRows(backpack, rows + 1);
        if (result != null) {
            e.setResult(result);
            e.getView().setRepairCost(3);
        }
    }
}
