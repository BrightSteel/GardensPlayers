package com.gardensmc.gardensplayers.backpack;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.menu.BackpackGui;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

@AllArgsConstructor
public class BackpackHandler {

    // player uuid -> backpack id
    private static final Map<UUID, UUID> openBackpacks = new HashMap<>();

    private static final String BACKPACK_KEY = "GardensBackpack";
    private static final String BACKPACK_ID_KEY = "Id";
    private static final String BACKPACK_ROWS_KEY = "Rows";

    private final ItemStack itemStack;
    private final CustomBackpackItem backpackItem;

    public static void clearBackpackFromMap(Player player) {
        openBackpacks.remove(player.getUniqueId());
    }

    public static void closeBackpackIfOpen(Player player, ItemStack backpack) {
        var backpackId = getBackpackId(backpack);
        if (backpackId == null) {
            return;
        }
        var openBackpack = openBackpacks.get(player.getUniqueId());
        if (openBackpack != null && openBackpack.equals(backpackId) && player.getOpenInventory().getTopInventory().getHolder() instanceof BackpackGui backpackGui) {
            onClose(player, backpackGui, backpack);
            player.getOpenInventory().close();
        }
    }

    /**
     * Whether an item may be stored inside a backpack - no backpacks, shulkers or bundles.
     */
    public static boolean isAllowedInBackpack(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }
        var typeStr = item.getType().toString().toLowerCase();
        if (typeStr.endsWith("shulker_box") || typeStr.endsWith("bundle")) {
            return false;
        }
        return !(GardensFurniture.itemRegistry.getCustomItem(item) instanceof CustomBackpackItem);
    }

    @Nullable
    private static UUID getBackpackId(ItemStack itemStack) {
        return NBT.get(itemStack, nbt -> {
            var compound = nbt.getCompound(BACKPACK_KEY);
            if (compound != null && compound.hasTag(BACKPACK_ID_KEY)) {
                return compound.getUUID(BACKPACK_ID_KEY);
            }
            return null;
        });
    }

    public ItemStack getUpgradeRowsResult() {
        var backpackData = getOrCreateBackpackData();
        // check if max met
        if (backpackData.rows == 6) {
            return null;
        }
        var clone = itemStack.clone();
        NBT.modify(clone, nbt -> {
            Objects.requireNonNull(nbt.getCompound(BACKPACK_KEY)).setInteger(BACKPACK_ROWS_KEY, backpackData.rows + 1);
        });
        return clone;
    }

    @Nullable
    public static BackpackHandler create(ItemStack itemStack) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (customItem instanceof CustomBackpackItem customBackpackItem) {
            return new BackpackHandler(itemStack, customBackpackItem);
        }
        return null;
    }

    public void openBackpack(Player player, boolean isWardrobeBackpack) {
        var backpackData = getOrCreateBackpackData();
        new BackpackGui(player, BackpackGui.RowConfig.fromRows(backpackData.rows), itemStack, backpackData.items, isWardrobeBackpack).open();
        openBackpacks.put(player.getUniqueId(), backpackData.id);
    }

    private record BackpackData(UUID id, int rows, ItemStack[] items) {
    }

    private BackpackData getOrCreateBackpackData() {
        var data = itemStack.getData(DataComponentTypes.CONTAINER);
        return NBT.modify(itemStack, nbt -> {
            var compound = nbt.getOrCreateCompound(BACKPACK_KEY);
            if (!compound.hasTag(BACKPACK_ID_KEY)) {
                compound.setUUID(BACKPACK_ID_KEY, UUID.randomUUID());
            }
            if (!compound.hasTag(BACKPACK_ROWS_KEY)) {
                compound.setInteger(BACKPACK_ROWS_KEY, backpackItem.getRows());
            }
            return new BackpackData(compound.getUUID(BACKPACK_ID_KEY), compound.getInteger(BACKPACK_ROWS_KEY), data == null ? new ItemStack[0] : data.contents().toArray(ItemStack[]::new));
        });
    }

    public static boolean isOpenBackpack(PlayerInventory inventory, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        var openBackpack = openBackpacks.get(Objects.requireNonNull(inventory.getHolder()).getUniqueId());
        if (openBackpack == null) {
            return false;
        }
        if (!(GardensFurniture.itemRegistry.getCustomItem(itemStack) instanceof CustomBackpackItem)) {
            return false;
        }
        return Objects.equals(openBackpack, getBackpackId(itemStack));
    }

    public static void onClose(Player player, BackpackGui backpackGui, ItemStack backpack) {
        if (!backpackGui.markClosed()) {
            return; // already saved - closeBackpackIfOpen saves, then the close event fires again
        }
        if (backpack == null || backpack.getType() == Material.AIR) {
            // AIR if closed after dropping item, but fine since we run this prior manually
            openBackpacks.remove(player.getUniqueId());
            return;
        }
        // Anything that slipped past the click checks (backpacks, shulkers, bundles) goes back to the player
        // rather than being serialized into the backpack.
        var disallowed = backpackGui.removeItems(item -> !isAllowedInBackpack(item));
        // If the open backpack itself was moved into its gui (eg a hotbar key swap), the stack we hold is now
        // stranded in the gui rather than the player's inventory. Save into the rescued stack and hand it back.
        var backpackId = getBackpackId(backpack);
        var target = disallowed.stream()
                .filter(item -> backpackId != null && backpackId.equals(getBackpackId(item)))
                .findFirst()
                .orElse(backpack);
        serializeItems(target, backpackGui.getItemContents());
        returnToPlayer(player, disallowed);
        if (backpackGui.isWardrobeBackpack()) {
            // save changes to db. The contents were written into the cached wardrobe stack itself, so if this fails
            // the player still sees them and the next close saves again; the nbt is logged so staff can restore it
            var nbt = NBT.itemStackToNBT(target).toString();
            try {
                new PlayerEntryHandler(player.getUniqueId()).updateWardrobe(CosmeticSlot.BACKPACK, nbt).get();
            } catch (Exception e) {
                GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to save wardrobe backpack of " + player.getName() + " (" + player.getUniqueId() + "): " + nbt, e);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Your backpack could not be saved. Don't log out - reopen and close it to try again, or contact staff."));
            }
        }
        openBackpacks.remove(player.getUniqueId());
    }

    private static void returnToPlayer(Player player, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        player.getInventory().addItem(items.toArray(ItemStack[]::new)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public static UUID serializeItems(ItemStack backpack, ItemStack[] contents) {
        backpack.setData(DataComponentTypes.CONTAINER, ItemContainerContents.containerContents(
                Arrays.stream(contents)
                        .map(item -> item == null ? new ItemStack(Material.AIR) : item)
                        .toList()
        ));
        var backpackId = NBT.modify(backpack, nbt -> {
            var compound = Objects.requireNonNull(nbt.getCompound(BACKPACK_KEY));
            return compound.getUUID(BACKPACK_ID_KEY);
        });

        return backpackId;
    }

    public static void registerUseListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!(event.getPlayer() instanceof Player player)) return;
                if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
                    // run on main thread for inventory opening
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                        // for some reason this event is triggered if you press 'drop' on an item inside an inventory, so lets not run if an inv is open
                        var openInv = player.getOpenInventory().getType();
                        if (openInv == InventoryType.CRAFTING || openInv == InventoryType.CREATIVE) { // no open inventory
                            var mainhand = player.getInventory().getItemInMainHand();
                            var offhand = player.getInventory().getItemInOffHand();
                            boolean openedBackpack = false;
                            if (mainhand.getType() != Material.AIR) {
                                openedBackpack = interactBackpack(player, mainhand, false);
                            }
                            if (player.isSneaking()) {
                                if (!openedBackpack && offhand.getType() != Material.AIR) {
                                    openedBackpack = interactBackpack(player, offhand, false);
                                }
                                if (!openedBackpack) {
                                    var wardrobeBackpack = PlayerEntryCache.INSTANCE.getSync(player.getUniqueId()).getWardrobe().getBackpack();
                                    if (wardrobeBackpack != null) {
                                        interactBackpack(player, wardrobeBackpack, true);
                                    }
                                }
                            }
                        }
                    });
                }
            }

            private boolean interactBackpack(Player player, ItemStack potentialBackpack, boolean isWardrobeBackpack) {
                if (potentialBackpack != null && GardensFurniture.itemRegistry.getCustomItem(potentialBackpack) instanceof CustomBackpackItem backpackItem) {
                    if (potentialBackpack.getAmount() == 1) { // prevent stacked from opening
                        new BackpackHandler(potentialBackpack, backpackItem).openBackpack(player, isWardrobeBackpack);
                        return true;
                    }
                }
                return false;
            }

        });
    }
}
