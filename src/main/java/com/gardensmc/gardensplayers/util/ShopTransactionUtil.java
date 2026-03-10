package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.currency.Currency;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;

@AllArgsConstructor
public class ShopTransactionUtil {

    private final Currency currency;

    public boolean tryPurchase(Player player, int cost, ItemStack item, Component itemName, int amount) {
        if (currency.getCurrency(player) >= cost) {
            try {
                currency.chargeCurrency(player, cost).get();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
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
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("You bought <red>%s x ".formatted(amount))
                    .append(itemName)
                    .append(MiniMessage.miniMessage().deserialize(" <white>for <red>" + cost + " " + currency.symbol())));
            return true;
        } else {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("You need <red>%s %s <white>to buy <red>%s x ".formatted(cost, currency.symbol(), amount))
                    .append(itemName));
            return false;
        }
    }

    public boolean trySell(Player player, int price, ItemStack item, Component itemName, int amount) {
        if (hasItems(player, item, amount)) {
            takeItems(player, item, amount);
            try {
                currency.addCurrency(player, price).get();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("You sold <green>%s x ".formatted(amount))
                    .append(itemName)
                    .append(MiniMessage.miniMessage().deserialize(" <white>for <green>" + price + " " + currency.symbol())));
            return true;
        }
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("You don't have <red>%s x ".formatted(amount))
                .append(itemName)
                .append(MiniMessage.miniMessage().deserialize(" <white>to sell.")));
        return false;
    }

    public int getItemCount(Player player, ItemStack item) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        return Arrays.stream(player.getInventory().getContents()).reduce(
                0,
                (accum, itemStack) -> isSameItem(customItem, item, itemStack) ? accum + itemStack.getAmount() : accum,
                Integer::sum
        );
    }

    private boolean hasItems(Player player, ItemStack item, int amount) {
        return getItemCount(player, item) >= amount;
    }

    private void takeItems(Player player, ItemStack item, int amount) {
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

    private boolean isSameItem(@Nullable CustomItem c1, ItemStack itemStack1, @Nullable ItemStack itemStack2) {
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
