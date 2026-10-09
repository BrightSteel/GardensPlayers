package com.gardensmc.gardensplayers.wardrobe;

import com.enderbeam.enderwardrobe.api.LegacyItemReader;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * GardensFurniture's old backpacks and balloon, for EnderWardrobe to turn into its own (its items.yml has them now).
 * Read straight off the item's GardensFurniture id, so they're still known once GardensFurniture no longer defines
 * them; a backpack's extra rows from an Expansion Rune (GardensBackpack.Rows) carry over.
 */
final class OldWearables implements LegacyItemReader {

    @Override
    @Nullable
    public Legacy read(ItemStack item) {
        String id = NBT.get(item, nbt -> nbt.hasTag(ItemRegistry.FURNITURE_KEY) ? nbt.getString(ItemRegistry.FURNITURE_KEY) : null);
        if (id == null) return null;
        if (id.equals("balloon")) return new Legacy(id, 0);
        if (!id.equals("backpack") && !(id.startsWith("backpack_") && !id.endsWith("_first_person"))) return null;
        Integer rows = NBT.get(item, nbt -> {
            var compound = nbt.getCompound("GardensBackpack");
            return compound != null && compound.hasTag("Rows") ? compound.getInteger("Rows") : null;
        });
        return new Legacy(id, rows == null ? 0 : rows);
    }

    @Override
    public void forget(ItemStack item) {
        NBT.modify(item, nbt -> {
            nbt.removeKey(ItemRegistry.FURNITURE_KEY);
        });
    }
}
