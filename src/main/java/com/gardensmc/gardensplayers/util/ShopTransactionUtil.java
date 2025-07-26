package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.GardensPlayers;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ShopTransactionUtil {

    public static void tryPurchase(Player player, int cost, ItemStack item, String shopItemName, int amount) {
        if (GardensPlayers.economy.has(player, cost)) {
            GardensPlayers.economy.withdrawPlayer(player, cost);
            var stacks = amount / 64;
            var remainder = amount % 64;
            IntStream.range(0, stacks).forEach(i -> {
                var stack = item.clone();
                stack.setAmount(64);
                player.give(stack);
            });
            if (remainder > 0) {
                var stack = item.clone();
                stack.setAmount(remainder);
                player.give(stack);
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("You bought <red>%s x %s <white>for <red>$%s".formatted(amount, shopItemName, cost)));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize("You need <red>$%s <white>to buy <red>%s x %s<white>.".formatted(cost, amount, shopItemName)));
        }
    }

    public static boolean trySell(Player player, int price, ItemStack item, String shopItemName, int amount) {
        if (hasItems(player, item, amount)) {
            takeItems(player, item, amount);
            GardensPlayers.economy.depositPlayer(player, price);
            player.sendMessage(MiniMessage.miniMessage().deserialize("You sold <green>%s x %s <white>for <green>$%s".formatted(amount, shopItemName, price)));
            return true;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("You don't have <red>%s x %s <white>to sell.".formatted(amount, shopItemName)));
        return false;
    }

    private static boolean hasItems(Player player, ItemStack item, int amount) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        var itemAmount = Arrays.stream(player.getInventory().getContents()).reduce(
                0,
                (accum, itemStack) -> isSameItem(customItem, item, itemStack) ? accum + itemStack.getAmount() : accum,
                Integer::sum
        );
        return itemAmount >= amount;
    }

    private static void takeItems(Player player, ItemStack item, int amount) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        var amountTaken = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (amountTaken == amount) {
                break;
            }
            if (isSameItem(customItem, item, invItem)) {
                var amountToTake = Math.min(invItem.getAmount(), amount - amountTaken);
                amountTaken += amountToTake;
                invItem.setAmount(invItem.getAmount() - amountToTake);
            }
        }
    }

    private static boolean isSameItem(@Nullable CustomItem c1, ItemStack itemStack1, @Nullable ItemStack itemStack2) {
        if (itemStack2 == null || itemStack2.getType() == Material.AIR) {
            return false; // fast break
        }
        var customItem2 = GardensFurniture.itemRegistry.getCustomItem(itemStack2);
        if (c1 != null) {
            return c1.equals(customItem2);
        } else if (customItem2 != null) {
            return false;
        }
        return itemStack1.getType() == itemStack2.getType();
    }
}
