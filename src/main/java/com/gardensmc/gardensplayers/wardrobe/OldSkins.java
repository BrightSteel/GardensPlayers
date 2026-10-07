package com.gardensmc.gardensplayers.wardrobe;

import com.enderbeam.enderwardrobe.api.LegacySkinReader;
import com.gardensmc.gardensfurniture.GardensFurniture;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Skins as GardensFurniture made them, before EnderWardrobe had skins: skin items were GardensFurniture items (their
 * GardensFurnitureId is the skin's id), and applying one wrote a FurnitureWrap compound (WrapId, and the item's own
 * Color) onto the item. EnderWardrobe uses this to make them over as its own.
 */
final class OldSkins implements LegacySkinReader {

    private static final String WRAP_KEY = "FurnitureWrap";
    private static final String WRAP_ID_KEY = "WrapId";
    private static final String COLOR_KEY = "Color";
    private static final String ID_KEY = "GardensFurnitureId";

    @Override
    public @Nullable String skinItemId(ItemStack item) {
        return NBT.get(item, nbt -> {
            if (nbt.hasTag(WRAP_KEY) || !nbt.hasTag(ID_KEY)) return null;
            var id = nbt.getString(ID_KEY);
            return id == null || id.isBlank() ? null : id;
        });
    }

    private record Wrap(String id, @Nullable Integer color) {
    }

    @Override
    public @Nullable Applied applied(ItemStack item, Function<String, @Nullable Component> skinName) {
        var wrap = NBT.get(item, nbt -> {
            var compound = nbt.getCompound(WRAP_KEY);
            if (compound == null || !compound.hasTag(WRAP_ID_KEY)) return null;
            return new Wrap(compound.getString(WRAP_ID_KEY), compound.hasTag(COLOR_KEY) ? compound.getInteger(COLOR_KEY) : null);
        });
        if (wrap == null) return null;
        var bare = item.clone();
        NBT.modify(bare, nbt -> {
            nbt.removeKey(WRAP_KEY);
        });
        // What the item looks like as made: a Gardens item (a backpack) as its item config builds it, anything else
        // (tools, weapons, elytra) as vanilla's.
        var custom = GardensFurniture.itemRegistry.getCustomItem(bare);
        var made = custom != null ? GardensFurniture.itemRegistry.buildItem(custom) : ItemStack.of(bare.getType());
        if (made == null) made = ItemStack.of(bare.getType());
        copy(made, bare, DataComponentTypes.ITEM_MODEL);
        copy(made, bare, DataComponentTypes.TOOLTIP_STYLE);
        if (bare.getType() == Material.ELYTRA) copy(made, bare, DataComponentTypes.EQUIPPABLE);
        if (wrap.color() != null) {
            bare.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor().color(Color.fromRGB(wrap.color())).build());
        } else {
            copy(made, bare, DataComponentTypes.DYED_COLOR);
        }
        // Named after the skin when it went on (unless a player had named it): back to its own name.
        var name = skinName.apply(wrap.id());
        var current = bare.getData(DataComponentTypes.CUSTOM_NAME);
        var plain = PlainTextComponentSerializer.plainText();
        if (name != null && current != null && plain.serialize(current).equals(plain.serialize(name))) {
            copy(made, bare, DataComponentTypes.CUSTOM_NAME);
        }
        return new Applied(wrap.id(), bare);
    }

    private static <T> void copy(ItemStack from, ItemStack to, DataComponentType.Valued<T> type) {
        var value = from.isDataOverridden(type) ? from.getData(type) : null;
        if (value != null) to.setData(type, value);
        else to.resetData(type);
    }
}
