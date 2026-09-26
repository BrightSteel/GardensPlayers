package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.currency.Currency;
import com.gardensmc.gardensplayers.shop.ShopLimitHandler;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;

@AllArgsConstructor
public class ShopTransactionUtil {

    private final Currency currency;

    public boolean tryPurchase(Player player, int cost, ItemStack item, Component itemName, int amount) {
        if (currency.getCurrency(player) >= cost) {
            try {
                if (!currency.chargeCurrency(player, cost).get()) {
                    // the cached balance was stale (e.g. a second purchase in the same tick)
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have enough to buy that"));
                    return false;
                }
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

    /**
     * Sells up to `requested` of `item` at `unitPrice` each, and is the only way the shops sell. It clamps to
     * what's left of today's limit (if any), requires the player to hold that many, pays for exactly that
     * amount, takes the items and then claims the limit - all in this call, on the main thread. Returns the
     * number sold (0 when nothing was sold; the player has been told why).
     */
    public int sell(Player player, int unitPrice, ItemStack item, Component itemName, int requested, @Nullable ShopLimitHandler limit) {
        if (requested <= 0) {
            return 0;
        }
        var amount = sellAmount(requested, limit == null ? null : limit.getAmountLeft());
        if (amount <= 0) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red>You've already met today's sell limit for ")
                    .append(itemName));
            return 0;
        }
        if (!hasItems(player, item, amount)) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("You don't have <red>%s x ".formatted(amount))
                    .append(itemName)
                    .append(MiniMessage.miniMessage().deserialize(" <white>to sell.")));
            return 0;
        }
        var price = unitPrice * amount;
        try {
            currency.addCurrency(player, price).get();
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // not paid: keep the items and the limit
        }
        takeItems(player, item, amount);
        if (limit != null) {
            limit.increaseAmountTransacted(amount);
        }
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("You sold <green>%s x ".formatted(amount))
                .append(itemName)
                .append(MiniMessage.miniMessage().deserialize(" <white>for <green>" + price + " " + currency.symbol())));
        return amount;
    }

    // how many of `requested` may be sold with `amountLeft` of today's limit (null: no limit)
    static int sellAmount(int requested, @Nullable Integer amountLeft) {
        return Math.max(0, amountLeft == null ? requested : Math.min(requested, amountLeft));
    }

    public int getItemCount(Player player, ItemStack item) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        return Arrays.stream(player.getInventory().getStorageContents()).reduce(
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
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
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
        // a plain vanilla shop item never matches gear or containers a player has put work into
        if (!itemStack2.getEnchantments().isEmpty()
                || (itemStack2.getItemMeta() instanceof BlockStateMeta blockStateMeta
                    && blockStateMeta.getBlockState() instanceof Container container && !container.getInventory().isEmpty())) {
            return false;
        }
        return itemStack1.getType() == itemStack2.getType();
    }
}
