package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.cache.ShopLimitKey;
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

    private final String shopItemName;
    private final ItemStack item;
    private final int basePrice;
    private final TransactionMode mode;
    private final ShopLimitHandler shopLimitHandler;
    private final Consumer<InventoryGui> onCancel;

    public ShopQuantityMenu(Player viewer, String shopItemName, ItemStack item, int basePrice, TransactionMode mode, ShopLimitHandler shopLimitHandler, Consumer<InventoryGui> onCancel) {
        super(layout, "<white>\uE105 \uE602", viewer);
        this.shopItemName = shopItemName;
        this.item = item;
        this.basePrice = basePrice;
        this.mode = mode;
        this.shopLimitHandler = shopLimitHandler;
        this.onCancel = onCancel;
        initElements();
    }

    public ShopQuantityMenu(Player viewer, String shopItemName, ItemStack item, int basePrice, TransactionMode mode, ShopLimitHandler shopLimitHandler) {
        this(viewer, shopItemName, item, basePrice, mode, shopLimitHandler, InventoryGui::close);
    }

    private void initElements() {
        gui.addElements(List.of(
                createSetQuantityButton('a', 1),
                createQuantityButton('b', -1),
                createQuantityButton('c', -10),
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
                            ShopTransactionUtil.tryPurchase(viewer, total, item, shopItemName, amount);
                        } else {
                            if (canSell(item.getAmount())) {
                                if (ShopTransactionUtil.trySell(viewer, total, item, shopItemName, amount)) {
                                    shopLimitHandler.increaseAmountTransacted(amount).thenRun(() -> onCancel.accept(gui));
                                    return true;
                                }
                            } else {
                                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot sell more than today's sell limit (" + shopLimitHandler.getLimit() + ")"));
                                return true;
                            }
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
                (mode == TransactionMode.BUY ? " <#82CD47>▸ <gray>ʙᴜʏ ᴘʀɪᴄᴇ <#82CD47>$" : " <#82CD47>▸ <gray>ѕᴇʟʟ ᴘʀɪᴄᴇ:  <#639bff>$") + basePrice,
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
        boolean isPositive = changeAmount > 0;
        return new DynamicGuiElement(character, () -> {
            var finalAmount =  item.getAmount() + changeAmount;
            finalAmount = Math.min(finalAmount, 64);
            finalAmount = Math.max(finalAmount, 1);
            int finalAmount1 = finalAmount;
            return new StaticGuiElement(
                character,
                blankItem,
                click -> {
                    item.setAmount(finalAmount1);
                    gui.draw();
                    return true;
                },
                    (isPositive ? "<green><bold>Add " : "<#DF2E38><bold>Remove ") + Math.abs(changeAmount)
            );
        });
    }
}
