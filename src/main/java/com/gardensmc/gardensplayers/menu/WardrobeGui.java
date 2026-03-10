package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.menu.wardrobe.ArmorWardrobeSlot;
import com.gardensmc.gardensplayers.menu.wardrobe.CosmeticWardrobeSlot;
import com.gardensmc.gardensplayers.menu.wardrobe.WardrobeSlot;
import com.gardensmc.gardensplayers.util.FontShift;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WardrobeGui implements InventoryHolder {

    public static final WardrobeSlot BACKPACK_SLOT = new CosmeticWardrobeSlot(CosmeticSlot.BACKPACK);
    public static final Map<Integer, WardrobeSlot> WARDROBE_SLOTS = Map.of(
            2, new ArmorWardrobeSlot(EquipmentSlot.HEAD),
            11, new ArmorWardrobeSlot(EquipmentSlot.CHEST),
            20, new ArmorWardrobeSlot(EquipmentSlot.LEGS),
            29, new ArmorWardrobeSlot(EquipmentSlot.FEET),

            6, new CosmeticWardrobeSlot(CosmeticSlot.HELMET),
            15, BACKPACK_SLOT,
            24, new CosmeticWardrobeSlot(CosmeticSlot.OFFHAND),
            33, new CosmeticWardrobeSlot(CosmeticSlot.BALLOON)
    );

    private final Player viewer;
    private final Inventory inventory;

    public WardrobeGui(Player viewer) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 36, MiniMessage.miniMessage().deserialize("<white><font:gardens:default>" + FontShift.INSTANCE.getShift(-8) + "\u014B</font>"));
        WARDROBE_SLOTS.forEach((key, value) -> inventory.setItem(key, value.getItem(viewer)));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open() {
        viewer.openInventory(getInventory());
    }

    public void updateWardrobeSlot(int slot, WardrobeSlot wardrobeSlot) {
        wardrobeSlot.updateWardrobe(viewer, inventory.getItem(slot));
        // refresh modified slot
        inventory.setItem(slot, wardrobeSlot.getItem(viewer));
    }
}
