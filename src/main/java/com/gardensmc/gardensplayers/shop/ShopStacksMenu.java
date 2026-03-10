package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.util.ShopTransactionUtil;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ShopStacksMenu extends Menu {

    private static final String[] layout = new String[] {
            "         ",
            "         ",
            " aaaaaaa ",
            "         ",
            "   bbb   ",
            "         "
    };

    private final ShopTransactionUtil transactionUtil = new ShopTransactionUtil(AcornsCurrencyHandler.INSTANCE);
    private final String shopItemName;
    private final ItemStack item;
    private final int basePrice;
    private final TransactionMode mode;
    private final ShopLimitHandler shopLimitHandler;
    private final Consumer<InventoryGui> quantityMenuOnCancel;

    public ShopStacksMenu(Player viewer, String shopItemName, ItemStack item, int basePrice, TransactionMode mode, ShopLimitHandler shopLimitHandler, Consumer<InventoryGui> quantityMenuOnCancel) {
        super(layout, "<white>\uE105 \uE604", viewer);
        this.shopItemName = shopItemName;
        this.item = item;
        this.basePrice = basePrice;
        this.mode = mode;
        this.shopLimitHandler = shopLimitHandler;
        this.quantityMenuOnCancel = quantityMenuOnCancel;
        initElements();
    }

    private void initElements() {
        var grp = new GuiElementGroup('a');
        grp.addElements(IntStream.range(1, 8)
                .mapToObj(this::createStackElement)
                .collect(Collectors.toList())
        );
        gui.addElement(grp);
        gui.addElement(createCancelElement());
    }

    private GuiElement createCancelElement() {
        return new StaticGuiElement('b', blankItem, click -> {
            open(() -> new ShopQuantityMenu(viewer, shopItemName, item, basePrice, mode, shopLimitHandler, quantityMenuOnCancel));
            return true;
        }, "<#DF2E38><bold>Cancel");
    }

    private GuiElement createStackElement(int stacks) {
        var stackItem = item.clone();
        stackItem.setAmount(stacks);
        var totalItems = stacks * 64;
        var price = totalItems * basePrice;
        return new StaticGuiElement('a', stackItem, click -> {
            if (mode == TransactionMode.BUY) {
                transactionUtil.tryPurchase(viewer, price, item, MiniMessage.miniMessage().deserialize(shopItemName), totalItems);
            } else {
                if (canSell(item.getAmount()) && transactionUtil.trySell(viewer, price, item, MiniMessage.miniMessage().deserialize(shopItemName), totalItems)) {
                    shopLimitHandler.increaseAmountTransacted(totalItems);
                    return true;
                } else {
                    viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot sell more than today's sell limit!"));
                }
            }
          return true;
        }, (mode == TransactionMode.BUY ? "<#82CD47><bold>Buy " : "<green><bold>Sell ") + stacks + " stack" + (stacks > 1 ? "s" : ""),
                " ",
                " <#82CD47>▸ <gray>ᴘʀɪᴄᴇ: <#82CD47>$" + price
        );
    }

    private boolean canSell(int amount) {
        if (shopLimitHandler == null) {
            return true;
        }
        return !shopLimitHandler.isLimitMet() && shopLimitHandler.isTransactionWithinLimit(amount);
    }
}
