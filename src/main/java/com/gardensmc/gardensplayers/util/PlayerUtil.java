package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensplayers.database.Tables;
import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftContainer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class PlayerUtil {

    @Nullable
    public static UUID getUUID(String username) {
        var promise = CompletableFuture.supplyAsync(() -> Tables.playerTable.getUUID(username));
        try {
            return promise.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void giveItems(Player player, ItemStack itemStack, int amount) {
        int stacks = amount / 64;
        int singles = amount % 64;
        var items = new ArrayList<ItemStack>();
        if (stacks > 0) {
            IntStream.range(0, stacks).forEach(i -> items.add(withAmount(itemStack, 64)));
        }
        if (singles > 0) {
            items.add(withAmount(itemStack, singles));
        }
        giveItems(player, items);
    }

    private static ItemStack withAmount(ItemStack item, int amount) {
        var clone = item.clone();
        clone.setAmount(amount);
        return clone;
    }

    public static void giveItems(Player player, Collection<ItemStack> items) {
        var emptySlots = getEmptyStorageSlots(player);
        if (items.size() <= emptySlots) {
            // presumably faster than individually adding each item?
            player.getInventory().addItem(items.toArray(ItemStack[]::new));
        } else {
            for (ItemStack item : items) {
                if (emptySlots > 0) {
                    emptySlots--;
                    player.getInventory().addItem(item);
                } else {
                    // drop by player
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
        }
    }

    private static int getEmptyStorageSlots(Player player) {
        return (int) Arrays.stream(player.getInventory().getStorageContents())
                .filter(Objects::isNull)
                .count();
    }

    /**
     * Copied from {@linkplain org.bukkit.craftbukkit.inventory.CraftInventoryView#sendInventoryTitleChange(InventoryView, String)}
     * and updated to accept a Component instead of plain text.
     * Note this does not update the CraftInventoryView's title variable, so all following calls to {@linkplain InventoryView#title()} will be inaccurate!
     *
     * @param view InventoryView to update
     * @param title Component to set inventory title to.
     */
    public static void sendInventoryTitleChange(InventoryView view, Component title) {
        Preconditions.checkArgument(view != null, "InventoryView cannot be null");
        Preconditions.checkArgument(title != null, "Title cannot be null");
        Preconditions.checkArgument(view.getPlayer() instanceof Player, "NPCs are not currently supported for this function");
        Preconditions.checkArgument(view.getTopInventory().getType().isCreatable(), "Only creatable inventories can have their title changed");
        ServerPlayer player = (ServerPlayer)((CraftHumanEntity)view.getPlayer()).getHandle();
        int containerId = player.containerMenu.containerId;
        net.minecraft.world.inventory.MenuType<?> windowType = CraftContainer.getNotchInventoryType(view.getTopInventory());
        player.connection.send(new ClientboundOpenScreenPacket(containerId, windowType, CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(title))));
        player.containerMenu.sendAllDataToRemote();
    }
}
