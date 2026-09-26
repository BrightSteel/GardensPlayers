package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.menu.wardrobe.ArmorWardrobeSlot;
import com.gardensmc.gardensplayers.menu.wardrobe.CosmeticWardrobeSlot;
import com.gardensmc.gardensplayers.menu.wardrobe.WardrobeSlot;
import com.gardensmc.gardensplayers.util.FontShift;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.geysermc.floodgate.api.FloodgateApi;
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
    public static final String TEMPLATE_PREFIX = "wardrobe_template_";

    private final Player viewer;
    private final Inventory inventory;
    private final boolean templateSlots;

    public WardrobeGui(Player viewer) {
        this.viewer = viewer;
        var isBedrockViewer = FloodgateApi.getInstance().isFloodgatePlayer(viewer.getUniqueId());
        this.templateSlots = isBedrockViewer;
        var title = isBedrockViewer ? Component.text("Wardrobe") : MiniMessage.miniMessage().deserialize("<white><font:gardens:default>" + FontShift.INSTANCE.getShift(-8) + "\u014B</font>");
        this.inventory = Bukkit.createInventory(this, 36, title);
        WARDROBE_SLOTS.forEach((key, value) -> inventory.setItem(key, itemOrTemplate(value, value.getItem(viewer))));
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
        refreshSlot(slot, wardrobeSlot);
    }

    // show what the player is actually wearing, without writing the GUI's copy back to them
    public void refreshSlot(int slot, WardrobeSlot wardrobeSlot) {
        inventory.setItem(slot, itemOrTemplate(wardrobeSlot, wardrobeSlot.getItem(viewer)));
    }

    /**
     * Returns the slot's real item, or the slot's template when the slot is empty and this
     * viewer uses template slots (Bedrock). Java viewers get a bare empty slot.
     */
    private ItemStack itemOrTemplate(WardrobeSlot wardrobeSlot, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) { // armor will be air - want to make sure template gets populated
            return item;
        }
        return templateSlots ? wardrobeSlot.getTemplate() : null;
    }

    public boolean isTemplate(ItemStack itemStack) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        return customItem != null && customItem.getIdentifier().startsWith(TEMPLATE_PREFIX);
    }
}
