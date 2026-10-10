package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.menu.shop.ShopItem;
import com.gardensmc.gardensplayers.shop.ShopLimitHandler;
import com.gardensmc.gardensplayers.util.ShopTransactionUtil;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TileShopPurchaseMenu extends Menu {

    private static final String[] layout = new String[] {
            "      ij ",
            "         ",
            " aa   bb ",
            " cc d ee ",
            " ff   gg ",
            "   hhh   "
    };

    private final ShopTransactionUtil transactionUtil;
    private final ShopItem shopItem;
    private final ItemStack item;
    private final TileMenu prevMenu;
    private final int prevPageNumber;
    private final boolean isBuy;
    private final boolean hasQuantityButtons;

    public TileShopPurchaseMenu(Player viewer, ShopItem shopItem, ItemStack shopItemStack, TileMenu tileMenu, int prevPageNumber, boolean isBuy, boolean hasQuantityButtons) {
        super(layout, buildTitle(hasQuantityButtons, isBuy), viewer);
        this.transactionUtil = new ShopTransactionUtil(shopItem.getCurrency().getCurrencyHandler(), "shop", shopItem.getShop(), shopItem.itemKey());
        this.shopItem = shopItem;
        this.item = shopItemStack;
        this.prevMenu = tileMenu;
        this.prevPageNumber = prevPageNumber;
        this.isBuy = isBuy;
        this.hasQuantityButtons = hasQuantityButtons;
        initElements();
    }

    private static String buildTitle(boolean hasQuantityButtons, boolean isBuy) {
        char menuChar;
        if (hasQuantityButtons) {
            menuChar = isBuy ? '\u013E' : '\u013F';
        } else {
            menuChar = isBuy ? '\u0145' : '\u0146';
        }
        return "<white><font:gardens:default>" + getShift(-8) + menuChar;
    }

    private void initElements() {
        gui.addElement(new StaticGuiElement(hasQuantityButtons ? 'j' : 'i', blankItem, click -> {
            openPrev();
            return true;
        }, "Back"));
        if (hasQuantityButtons) {
            gui.addElements(
                    buildAmountModifierElement('a', -1),
                    buildAmountModifierElement('b', 1),
                    buildAmountModifierElement('c', -10),
                    buildAmountModifierElement('e', 10),
                    buildAmountModifierElement('f', -32),
                    buildAmountModifierElement('g', 32)
            );
        }
        gui.addElement(new DynamicGuiElement('d', () -> new StaticGuiElement(
                'd',
                item
        )));
        if (isBuy) {
            gui.addElement(new DynamicGuiElement('h', () -> {
                var price = item.getAmount() * shopItem.getBuyPrice();
                return new StaticGuiElement('h', blankItem, click -> {
                if (transactionUtil.tryPurchase(viewer, price, item, item.displayName(), item.getAmount())) {
                    openPrev();
                }
                return true;
                }, "<bold><green>Buy </bold><white>x" + item.getAmount(),
                        "<#e7c86b>Purchase: <#e1e1e2>" + price + " " + shopItem.getCurrency().getCurrencyHandler().symbol(),
                        " ",
                        "Click to buy!"
                );
            }));
        } else {
            gui.addElement(new DynamicGuiElement('h', () -> {
                var price = item.getAmount() * shopItem.getSellPrice();
                return new StaticGuiElement('h', blankItem, click -> {
                    // only limited items are sold here; sell() clamps to what's left and pays for what was sold
                    if (shopItem.hasSellLimit()) {
                        var sellLimitHandler = new ShopLimitHandler(viewer.getUniqueId(), shopItem.getId(), shopItem.getSellLimit());
                        if (transactionUtil.sell(viewer, shopItem.getSellPrice(), item, item.displayName(), item.getAmount(), sellLimitHandler) > 0) {
                            openPrev();
                        }
                    }
                    return true;
                    }, "<bold><red>Sell </bold><white>x" + item.getAmount(),
                            "<#e7c86b>Sell: <#e1e1e2>" + price + " " + shopItem.getCurrency().getCurrencyHandler().symbol(),
                            " ",
                            "Click to sell!"
                    );
            }));
        }
    }

    private void openPrev() {
        InventoryGui.clearHistory(viewer);
        prevMenu.reopen();
    }

    private GuiElement buildAmountModifierElement(char c, int modifier) {
        return buildQuantityButton(c, item, modifier, (modifier > 0 ? "+" : "-") + Math.abs(modifier));
    }
}
