package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.menu.Menu;
import com.gardensmc.gardensplayers.util.ShopTransactionUtil;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class ShopQuantityMenu extends Menu {

    private static final String[] layout = new String[] {
            "         ",
            "         ",
            "abc d efg",
            "         ",
            "  hh ii  ",
            "    j    "
    };

    private final ShopTransactionUtil transactionUtil;
    private final String shopItemName;
    private final ItemStack item;
    private final int basePrice;
    private final TransactionMode mode;
    private final ShopLimitHandler shopLimitHandler;
    private final Consumer<InventoryGui> onCancel;

    public ShopQuantityMenu(Player viewer, String shopItemName, ItemStack item, int basePrice, TransactionMode mode, ShopLimitHandler shopLimitHandler, Consumer<InventoryGui> onCancel) {
        super(layout, "\uE105 \uE602" /* Menu adds <white> */, viewer);
        this.shopItemName = shopItemName;
        this.item = item;
        this.basePrice = basePrice;
        this.mode = mode;
        this.shopLimitHandler = shopLimitHandler;
        // the farm shop always counts a limit; the region furniture shops (FurnitureListener) have none
        this.transactionUtil = new ShopTransactionUtil(AcornsCurrencyHandler.INSTANCE,
                shopLimitHandler == null ? "furniture_shop" : "farm_shop", null, null);
        this.onCancel = onCancel;
        initElements();
    }

    public ShopQuantityMenu(Player viewer, String shopItemName, ItemStack item, int basePrice, TransactionMode mode, ShopLimitHandler shopLimitHandler) {
        this(viewer, shopItemName, item, basePrice, mode, shopLimitHandler, InventoryGui::close);
    }

    private void initElements() {
        gui.addElements(List.of(
                createSetQuantityButton('a', 1),
                createQuantityButton('b', -10),
                createQuantityButton('c', -1),
                createQuantityButton('e', 1),
                createQuantityButton('f', 10),
                createSetQuantityButton('g', 64),
                createConfirmButton('h', false),
                new DynamicGuiElement('i', () -> createConfirmButton('i', true)),
                createItemDisplayButton(),
                createStacksButton()
        ));
    }

    private GuiElement createStacksButton() {
        return new StaticGuiElement('j', blankItem, click -> {
            open(() -> new ShopStacksMenu(viewer, shopItemName, item, basePrice, mode, shopLimitHandler, onCancel));
            return true;
        }, "<green><bold>" + mode.getText() + " more");
    }

    private GuiElement createConfirmButton(char character, boolean isPurchase) {
        return new StaticGuiElement(
                character,
                blankItem,
                click -> {
                    var amount = item.getAmount();
                    var total = amount * basePrice;
                    if (isPurchase) {
                        if (mode == TransactionMode.BUY) {
                            transactionUtil.tryPurchase(viewer, total, item, MiniMessage.miniMessage().deserialize(shopItemName), amount);
                        } else {
                            if (transactionUtil.sell(viewer, basePrice, item, MiniMessage.miniMessage().deserialize(shopItemName), amount, shopLimitHandler) > 0) {
                                onCancel.accept(gui);
                            }
                            return true;
                        }
                    }
                    onCancel.accept(gui);
                    return true;
                },
                isPurchase ? "<green><bold>Confirm" : "<#DF2E38><bold>Cancel",
                isPurchase && !canSell(item.getAmount()) ? "<red>Cannot sell past stock limit" : ""
        );
    }

    private boolean canSell(int amount) {
        if (shopLimitHandler == null) {
            return true;
        }
        return !shopLimitHandler.isLimitMet() && shopLimitHandler.isTransactionWithinLimit(amount);
    }

    private GuiElement createItemDisplayButton() {
        return new DynamicGuiElement('d', () -> new StaticGuiElement(
                'd',
                item,
                shopItemName,
                "<gray>Quantity: <#82CD47>" + item.getAmount(),
                " ",
                (mode == TransactionMode.BUY ? " <#82CD47>▸ <gray>ʙᴜʏ ᴘʀɪᴄᴇ <#82CD47>$" : " <#82CD47>▸ <gray>ѕᴇʟʟ ᴘʀɪᴄᴇ:  <#639bff>$") + (basePrice * item.getAmount()),
                (mode == TransactionMode.SELL) ? " <#82CD47>▸ <gray>ꜱᴛᴏᴄᴋ: " + (shopLimitHandler.getAmountLeft() == 0 ? "<red>" : "<#639bff>") + shopLimitHandler.getAmountLeft() : "",
                canSell(item.getAmount()) ? "" : "<red>Cannot sell past stock limit"
        ));
    }

    private GuiElement createSetQuantityButton(char character, int amount) {
        return new StaticGuiElement(character, blankItem, click -> {
            item.setAmount(amount);
            gui.draw();
            return true;
        }, (amount == 1 ? "<#82CD47>" : "<green>") + "<bold>Set to " + amount);
    }

    private GuiElement createQuantityButton(char character, int changeAmount) {
        return buildQuantityButton(character, item, changeAmount,
                (changeAmount > 0 ? "<green><bold>Add " : "<#DF2E38><bold>Remove ") + Math.abs(changeAmount));
    }
}
