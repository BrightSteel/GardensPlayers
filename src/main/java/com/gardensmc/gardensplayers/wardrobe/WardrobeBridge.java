package com.gardensmc.gardensplayers.wardrobe;

import com.enderbeam.enderwardrobe.api.WardrobeApi;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensplayers.database.DatabaseConnector;
import com.gardensmc.gardensplayers.database.entry.MySQLEntry;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * GardensMC's side of EnderWardrobe, which owns the backpack and balloon slots and items, skins and backpack storage:
 * tells it which Gardens items go in the slots, how GardensFurniture's old skins, backpacks and balloons were made (for
 * it to make them its own), and once moves the old wardrobes (this plugin's wardrobes table) over to it.
 */
public final class WardrobeBridge {

    private static WardrobeApi api;

    private WardrobeBridge() {
    }

    /** A new item of an EnderWardrobe skin (skins.yml id), for the shop; null without EnderWardrobe or such a skin. */
    @org.jetbrains.annotations.Nullable
    public static org.bukkit.inventory.ItemStack skinItem(String id) {
        return api == null ? null : api.skinItem(id);
    }

    public static void enable(Plugin plugin) {
        api = Bukkit.getServicesManager().load(WardrobeApi.class);
        if (api == null) {
            plugin.getLogger().severe("EnderWardrobe isn't running: no backpack or balloon slots.");
            return;
        }
        api.addProvider(WardrobeBridge::slotOf);
        api.addLegacyReader(new OldSkins());
        api.addLegacyItemReader(new OldWearables());
        api.addBackpackProvider(WardrobeBridge::rowsOf);
        importOldWardrobes(plugin);
    }

    /**
     * Storage backpacks (skinned or not) go in the backpack slot; other items where their item config's
     * {@code wardrobe_slot} says.
     */
    @Nullable
    private static String slotOf(ItemStack item) {
        var custom = GardensFurniture.itemRegistry.getCustomItem(item);
        if (custom == null) return null;
        if (custom instanceof CustomBackpackItem) return "backpack";
        return custom.getWardrobeSlot();
    }

    /**
     * GardensFurniture's backpacks hold their item config's rows, or as many as an Expansion Rune gave them back when
     * this plugin stored their contents (GardensBackpack.Rows).
     */
    private static int rowsOf(ItemStack item) {
        if (!(GardensFurniture.itemRegistry.getCustomItem(item) instanceof CustomBackpackItem backpack)) return 0;
        Integer upgraded = NBT.get(item, nbt -> {
            var compound = nbt.getCompound("GardensBackpack");
            return compound != null && compound.hasTag("Rows") ? compound.getInteger("Rows") : null;
        });
        return upgraded != null && upgraded > 0 ? upgraded : backpack.getRows();
    }

    /** How many rows a backpack holds now, or 0 for anything else. */
    public static int backpackRows(ItemStack item) {
        return api == null ? 0 : api.backpackRows(item);
    }

    @Nullable
    public static ItemStack withBackpackRows(ItemStack backpack, int rows) {
        return api == null ? null : api.withBackpackRows(backpack, rows);
    }

    // ---- Moving the old wardrobes over ----

    private static final class OldWardrobe extends MySQLEntry {
        String owner, helmet, backpack, offhand, balloon;

        @Override
        public OldWardrobe populate(ResultSet row) throws SQLException {
            owner = row.getString("playerUUID");
            helmet = row.getString("helmet");
            backpack = row.getString("backpack");
            offhand = row.getString("offhand");
            balloon = row.getString("balloon");
            return this;
        }
    }

    /**
     * Each old wardrobe's backpack and balloon become worn in EnderWardrobe (or go to the stash if something is worn
     * there already); hats and offhand cosmetics, which have no slot now, go to the stash. Each item is cleared from
     * the old wardrobe as soon as it has moved, so this runs on every start, only ever moves what's left, and never
     * moves anything twice. An item that can't be read or saved is left where it was.
     */
    private static void importOldWardrobes(Plugin plugin) {
        var log = plugin.getLogger();
        try {
            var rows = DatabaseConnector.INSTANCE.queryList(
                    "SELECT * FROM wardrobes WHERE helmet IS NOT NULL OR backpack IS NOT NULL OR offhand IS NOT NULL OR balloon IS NOT NULL",
                    OldWardrobe::new);
            int moved = 0, left = 0;
            for (var row : rows) {
                var owner = UUID.fromString(row.owner);
                for (var column : new String[]{"backpack", "balloon", "helmet", "offhand"}) {
                    var nbt = switch (column) {
                        case "backpack" -> row.backpack;
                        case "balloon" -> row.balloon;
                        case "helmet" -> row.helmet;
                        default -> row.offhand;
                    };
                    if (nbt == null || nbt.isBlank()) continue;
                    var item = item(nbt, owner, log);
                    boolean ok = item != null && (column.equals("backpack") || column.equals("balloon")
                            ? api.importWorn(owner, column, item)
                            : api.importToStash(owner, item));
                    if (ok || item == null && isEmptyItem(nbt)) {
                        // column is one of four fixed names, never input
                        DatabaseConnector.INSTANCE.update("UPDATE wardrobes SET " + column + " = NULL WHERE playerUUID = ?", row.owner);
                        if (ok) moved++;
                    } else {
                        left++;
                    }
                }
            }
            if (moved + left > 0) {
                log.info("Moved " + moved + " old wardrobe items to EnderWardrobe" + (left > 0 ? "; " + left + " couldn't be and were left to try again next start." : "."));
            }
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Couldn't read the old wardrobes to move them to EnderWardrobe; they're left as they were.", e);
        }
    }

    private static boolean isEmptyItem(String nbt) {
        try {
            var item = NBT.itemStackFromNBT(NBT.parseNBT(nbt));
            return item == null || item.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private static ItemStack item(@Nullable String nbt, UUID owner, java.util.logging.Logger log) {
        if (nbt == null || nbt.isBlank()) return null;
        try {
            var item = NBT.itemStackFromNBT(NBT.parseNBT(nbt));
            return item == null || item.isEmpty() ? null : item;
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't read an old wardrobe item of " + owner + ": " + nbt, e);
            return null;
        }
    }
}
