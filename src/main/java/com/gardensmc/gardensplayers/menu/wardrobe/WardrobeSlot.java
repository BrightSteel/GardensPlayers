package com.gardensmc.gardensplayers.menu.wardrobe;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface WardrobeSlot {

    ItemStack getItem(Player player);

    boolean isValidItem(ItemStack itemStack);

    void updateWardrobe(Player player, ItemStack itemStack);

    ItemStack getTemplate();

}
