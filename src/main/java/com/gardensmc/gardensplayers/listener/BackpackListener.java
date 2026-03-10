package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.backpack.BackpackHandler;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Set;

public class BackpackListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractBackpack(PlayerInteractEvent e) {

    }



    @EventHandler
    public void onSlotRuneBackpack(PrepareAnvilEvent e) {
        var backpack = e.getInventory().getFirstItem();
        var rune = e.getInventory().getSecondItem();
        if (backpack == null || rune == null) {
            return;
        }
        var backpackItem = GardensFurniture.itemRegistry.getCustomItem(backpack);
        if (!(backpackItem instanceof CustomBackpackItem customBackpackItem)) {
            return;
        }
        var runeItem = GardensFurniture.itemRegistry.getCustomItem(rune);
        if (!(runeItem instanceof CustomItem customItem) || !customItem.getIdentifier().equals("expansion_rune")) {
            return;
        }
        var result = new BackpackHandler(backpack, customBackpackItem).getUpgradeRowsResult();
        if (result != null) {
            e.setResult(result);
            e.getView().setRepairCost(3);
        }
    }
}
