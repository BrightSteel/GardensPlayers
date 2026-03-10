package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.menu.TileShopPurchaseMenu;
import com.gardensmc.gardensplayers.menu.shop.ShopItem;
import com.gardensmc.gardensplayers.shop.ShopLimitHandler;
import com.gardensmc.gardensplayers.util.ShopTransactionUtil;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class ShopTileItem extends TileItem {

    private final ShopItem shopItem;

    public ShopTileItem(ShopItem shopItem) {
        super(shopItem.getItemStack() == null || shopItem.getItemStack().getType() == Material.AIR);
        this.shopItem = shopItem;
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        return toTileItem(viewer, shopItem);
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        if (getTileColor(player) == TileColor.RED) {
            return;
        }
        var clickType = click.getType();
        ShopLimitHandler sellLimitHandler = shopItem.hasSellLimit()
                ? new ShopLimitHandler(player.getUniqueId(), shopItem.getId(), shopItem.getSellLimit())
                : null;
        if ((clickType.isRightClick() || clickType == ClickType.DROP) && sellLimitHandler != null) {
            if (sellLimitHandler.isLimitMet()) {
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<red>You've already met today's sell limit for ")
                        .append(shopItem.getItemStack().displayName()));
                return;
            }
        }
        if ((clickType.isLeftClick() && shopItem.getBuyPrice() != -1) || (clickType.isRightClick() && shopItem.getSellPrice() != -1)) {
            InventoryGui.clearHistory(player);
            var itemStack = shopItem.getItemStack();
            new TileShopPurchaseMenu(player, shopItem, itemStack, tileMenu, click.getGui().getPageNumber(player), clickType.isLeftClick(), itemStack.getMaxStackSize() > 1).open();
        } else if (clickType == ClickType.DROP && shopItem.getSellPrice() != -1) {
            var itemStack = shopItem.getItemStack();
            var transactionUtil = new ShopTransactionUtil(shopItem.getCurrency().getCurrencyHandler());
            var items = transactionUtil.getItemCount(player, itemStack);
            if (sellLimitHandler != null) {
                items = Math.min(items, sellLimitHandler.getAmountLeft());
            }
            if (items > 0) {
                if (transactionUtil.trySell(player, shopItem.getSellPrice() * items, itemStack, itemStack.displayName(), items)) {
                    if (sellLimitHandler != null) {
                        sellLimitHandler.increaseAmountTransacted(items).thenRun(() -> {
                            InventoryGui.clearHistory(player);
                            tileMenu.reopen();
                        });
                    }
                }
            } else {
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<red>You don't have any ")
                        .append(itemStack.displayName())
                        .append(MiniMessage.miniMessage().deserialize(" <red>to sell")));
            }
        }
    }

    @Override
    public TileColor getTileColor(Player viewer) {
        if (shopItem.getSellPrice() == -1 && shopItem.getBuyPrice() != -1 && shopItem.getCurrency().getCurrencyHandler().getCurrency(viewer) < shopItem.getBuyPrice()) {
            return TileColor.RED;
        }
        return TileColor.GRAY;
    }

    private static ItemStack toTileItem(Player viewer, ShopItem shopItem) {
        var itemStack = shopItem.getItemStack();
        var buyPrice = shopItem.getBuyPrice();
        var sellPrice = shopItem.getSellPrice();
        var currency = shopItem.getCurrency().getCurrencyHandler();
        if (itemStack != null && !itemStack.isEmpty()) {
            var lore = itemStack.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(miniMessage(""));
            if (buyPrice != -1) {
                lore.add(miniMessage("<#e7c86b>Purchase: <#e1e1e2>" + buyPrice + " " + currency.symbol()));
            }
            if (sellPrice != -1) {
                var sellText = "<#e7c86b>Sell: <#e1e1e2>" + sellPrice + " " + currency.symbol();
                if (shopItem.getSellLimit() > -1) {
                    var sellLimitHandler = new ShopLimitHandler(viewer.getUniqueId(), shopItem.getId(), shopItem.getSellLimit());
                    sellText += " <gray>(Stock: %s)".formatted(sellLimitHandler.getAmountLeft());
                }
                lore.add(miniMessage(sellText));
            }
            lore.add(miniMessage(""));
            if (buyPrice != -1) {
                var canAfford = currency.getCurrency(viewer) >= buyPrice;
                lore.add(miniMessage(canAfford ? "<white><font:gardens:default>\u0143</font><#64ba3f> Left-click to buy!" : "<red>Not enough " + currency.name() + " to buy!"));
            }
            if (sellPrice != -1) {
                lore.add(miniMessage("<white><font:gardens:default>\u0144</font><#3fafd7> Right-click to sell!"));
                lore.add(miniMessage("<#e1e1e2>Drop (Q key) to sell all!"));
            }
            itemStack.lore(lore);
        }
        return itemStack;
    }

    private static Component miniMessage(String text) {
        return MiniMessage.miniMessage().deserialize("<!italic>" + text);
    }
}
