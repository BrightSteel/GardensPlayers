package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardenscrops.utils.ConfigUtil;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.menu.Menu;
import de.themoep.inventorygui.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FarmShopMenu extends Menu {

    private static String boostedCrop;

    private static final String[] layout = new String[] {
            "         ",
            " aaaa    ",
            " aaaa    ",
            "eaaaaf g ",
            " aaaa    ",
            "         "
    };
    public static final int DAILY_SELL_LIMIT = 192;
    private final ItemStack backButton = GardensFurniture.itemRegistry.buildItem("shop_back_button");
    private final ItemStack nextButton = GardensFurniture.itemRegistry.buildItem("shop_next_button");

    public FarmShopMenu(Player viewer) {
        super(layout, "\uE105 \uE106" /* Menu adds <white> */, viewer);
        initElements();
    }

    public FarmShopMenu(Player viewer, int page) {
        this(viewer);
        gui.setPageNumber(viewer, page);
    }

    private void initElements() {
        var grp = new GuiElementGroup('a');
        grp.addElements(ConfigUtil.cropShopItems.stream().flatMap(shopItem -> createGuiElement(shopItem).stream()).toList());
        gui.addElement(grp);
        gui.addElement(new GuiPageElement('f', nextButton, GuiPageElement.PageAction.NEXT, "Next page"));
        gui.addElement(new GuiPageElement('e', backButton, GuiPageElement.PageAction.PREVIOUS, "Previous page"));
        gui.addElement(createBoostedCropElement());
    }

    private GuiElement createBoostedCropElement() {
        var item = GardensFurniture.itemRegistry.buildItem(getBoostedCrop() + "_1star");
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        if (customItem == null) {
            return null;
        }
        return new StaticGuiElement(
                'g',
                item,
                "<blue>Boosted Crop",
                "<yellow>" + StringUtils.capitalize(getBoostedCrop()),
                " ",
                "<gray>Boosted crops receive a 25% sell bonus,",
                "<gray>and will change each day at 7 AM"
        );
    }

    private List<GuiElement> createGuiElement(ConfigUtil.CropShopItem cropShopItem) {
        var boosted = isBoostedCrop(cropShopItem);
        return List.of(
                createShopItem(cropShopItem.name() + "_seeds", cropShopItem.seedsPrice(), TransactionMode.BUY, false),
                createShopItem(cropShopItem.name() + "_1star", getStarPrice(cropShopItem, 1), TransactionMode.SELL, boosted),
                createShopItem(cropShopItem.name() + "_2star", getStarPrice(cropShopItem,2), TransactionMode.SELL, boosted),
                createShopItem(cropShopItem.name() + "_3star", getStarPrice(cropShopItem,3), TransactionMode.SELL, boosted)
        );
    }

    private int getStarPrice(ConfigUtil.CropShopItem cropShopItem, int stars) {
        var basePrice = cropShopItem.starPrice(stars);
        if (isBoostedCrop(cropShopItem)) {
            return basePrice + (int) Math.round(basePrice*0.25);
        }
        return basePrice;
    }

    private GuiElement createShopItem(String id, int basePrice, TransactionMode mode, boolean isBoosted) {
        var item = GardensFurniture.itemRegistry.buildItem(id);
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        if (customItem == null || item == null) {
            return new StaticGuiElement('a', null);
        }
        var shopItemName = customItem.getDisplayRawFormatted();
        var shopLimitHandler = new ShopLimitHandler(viewer.getUniqueId(), id, DAILY_SELL_LIMIT);

        var amountLeft = shopLimitHandler.getAmountLeft();
        return new StaticGuiElement(
                'a',
                item,
                click -> {
                    var pageNumber = gui.getPageNumber(viewer);
                    return open(() -> new ShopQuantityMenu(viewer, shopItemName, item, basePrice, mode, shopLimitHandler, inventoryGui -> open(() -> new FarmShopMenu(viewer, pageNumber))));
                },
                shopItemName,
                "",
                (mode == TransactionMode.BUY ? " <gray>▪ ʙᴜʏ ᴘʀɪᴄᴇ: <#82CD47>$" : " <gray>▪ ѕᴇʟʟ ᴘʀɪᴄᴇ: <#639bff>$") + basePrice,
                (mode == TransactionMode.SELL) ? " <gray>▪ ꜱᴛᴏᴄᴋ: " + (amountLeft == 0 ? "<red>" : "<#639bff>") + amountLeft : "",
                isBoosted ? " <#639bff>▪ ʙᴏᴏꜱᴛᴇᴅ" : ""
        );
    }

    private boolean isBoostedCrop(ConfigUtil.CropShopItem cropShopItem) {
        return getBoostedCrop().equals(cropShopItem.name());
    }

    private String getBoostedCrop() {
        if (boostedCrop == null) {
            var shopItems = ConfigUtil.cropShopItems;
            boostedCrop = shopItems.get(ThreadLocalRandom.current().nextInt(shopItems.size())).name();
        }
        return boostedCrop;
    }

    public static void resetBoostedCrop() {
        boostedCrop = null;
    }

}
