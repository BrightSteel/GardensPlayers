package com.gardensmc.gardensplayers.menu.wardrobe;

import com.gardensmc.gardensfurniture.GardensFurniture;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static com.gardensmc.gardensplayers.menu.WardrobeGui.TEMPLATE_PREFIX;

@Getter
public class ArmorWardrobeSlot implements WardrobeSlot {

    private final EquipmentSlot equipmentSlot;

    public ArmorWardrobeSlot(EquipmentSlot equipmentSlot) {
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public ItemStack getItem(Player player) {
        var equipment = player.getEquipment();
        return equipment == null ? null : equipment.getItem(equipmentSlot);
    }

    @Override
    public boolean isValidItem(ItemStack itemStack) {
        var equippable = itemStack.getData(DataComponentTypes.EQUIPPABLE);
        return equippable == null ? false : equippable.slot() == equipmentSlot;
    }

    @Override
    public void updateWardrobe(Player player, ItemStack itemStack) {
        var equipment = player.getEquipment();
        equipment.setItem(equipmentSlot, itemStack);
    }

    @Override
    public ItemStack getTemplate() {
        return GardensFurniture.itemRegistry.buildItem(TEMPLATE_PREFIX + equipmentSlot.name().toLowerCase());
    }
}
