package com.gardensmc.gardensplayers.menu.shop;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.currency.Currency;
import com.gardensmc.gardensplayers.shop.ShopLimitHandler;
import com.gardensmc.gardensplayers.util.ShopTransactionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Comparator;
import java.util.List;

/**
 * Geyser/Bedrock equivalent of the {@code /shop} tile menu, re-skinned by the bundled Bedrock JSON UI
 * pack (see {@code bedrock-resourcepack/}).
 *
 * <p>Flow: category grid -> item grid (icon + price + inner Buy) -> either the lore screen (main cell
 * click) or the buy window (inner Buy) -> Yes/No confirm.
 *
 * <p><b>Plugin&lt;-&gt;pack contract.</b> Titles carry hidden private-use glyphs the pack reads from
 * {@code #title_text} (rendered invisibly by the blank {@code font/glyph_E1.png}):
 * <ul>
 *   <li>{@link #GRID_TOKEN} marks the wide paginated grid forms (category + item grids).</li>
 *   <li>{@link #SHOP_TOKEN} marks the other shop forms (lore/detail/confirm) which use normal layouts.</li>
 * </ul>
 * For the item grid the buttons are sent in three fixed blocks so the pack's hardcoded cells can map a
 * cell to two actions: indices {@code 0..SLOTS-1} are the per-item "view" buttons (icon + price, click
 * opens lore), {@code SLOTS..2*SLOTS-1} are the matching "buy" buttons (the inner Buy strip), and the
 * final two are Prev / Next. Missing items/nav are sent as empty buttons so indices stay aligned.
 */
public class BedrockShopMenu {

    private static final String SHOP_TOKEN = "\uE110";
    private static final String GRID_TOKEN = "\uE111";
    private static final String CAT_TOKEN = "\uE112";

    private static final String DEFAULT_ICON = "textures/gardens/shop/default_item";
    private static final String NAV_PREV_ICON = "textures/gardens/shop/nav_prev";
    private static final String NAV_NEXT_ICON = "textures/gardens/shop/nav_next";
    private static final String BUY_ICON = "textures/gardens/shop/buy_button";
    private static final String SELL_ICON = "textures/gardens/shop/sell_button";
    private static final String ACORN_GLYPH = "\uE200";
    private static final String GEM_GLYPH = "\uE201";

    // Grid is GRID_COLS x GRID_ROWS cells per page; keep in sync with the pack's hardcoded cells.
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 3;
    private static final int SLOTS = GRID_COLS * GRID_ROWS;

    private BedrockShopMenu() {
    }

    public static void open(Player viewer) {
        sendShopSelectForm(viewer);
    }

    // 1. Category grid. Categories are plain "view" buttons (no buy block / nav), so the pack hides the
    // Buy strips for these cells automatically.
    private static void sendShopSelectForm(Player viewer) {
        var shops = GardensPlayers.shops;   // in shops/order.yml's order
        var builder = SimpleForm.builder().title(CAT_TOKEN + "Shops");
        shops.forEach(shop -> builder.button(plain(shop.getName()), FormImage.Type.PATH, categoryIcon(shop.getName())));
        builder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx < shops.size()) {
                sendItemGridForm(viewer, shops.get(idx), 0, false);
            }
        });
        sendForm(viewer, builder.build());
    }

    // 2. Item grid with a Buy/Sell mode. Per page: view block + strip block + Prev/Next + Buy/Sell tabs.
    // In sell mode only sellable items show (and vice versa); the strip & price reflect the mode.
    private static void sendItemGridForm(Player viewer, Shop shop) {
        sendItemGridForm(viewer, shop, 0, false);
    }

    private static void sendItemGridForm(Player viewer, Shop shop, int page, boolean sell) {
        var items = shop.getShopItems().stream()
                .filter(it -> isInMode(it, sell))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) SLOTS));
        int p = Math.max(0, Math.min(page, totalPages - 1));
        int from = p * SLOTS;
        var pageItems = items.subList(from, Math.min(from + SLOTS, items.size()));
        int count = pageItems.size();
        boolean hasPrev = p > 0;
        boolean hasNext = p < totalPages - 1;

        var builder = SimpleForm.builder()
                .title(GRID_TOKEN + plain(shop.getName()))
                .content("Page " + (p + 1) + " / " + totalPages);

        // 0..SLOTS-1: view buttons (icon + mode price), padded.
        for (int i = 0; i < SLOTS; i++) {
            if (i < count) {
                builder.button(gridButtonText(pageItems.get(i), sell), FormImage.Type.PATH, itemIcon(pageItems.get(i)));
            } else {
                builder.button("");
            }
        }
        // SLOTS..2*SLOTS-1: strip click targets (label + colour reflect the mode).
        String stripLabel = sell ? "Sell" : "Buy";
        String stripIcon = sell ? SELL_ICON : BUY_ICON;
        for (int i = 0; i < SLOTS; i++) {
            if (i < count) {
                builder.button(stripLabel, FormImage.Type.PATH, stripIcon);
            } else {
                builder.button("");
            }
        }
        // 2*SLOTS, +1: Prev / Next.
        builder.button(hasPrev ? "Prev" : "", FormImage.Type.PATH, hasPrev ? NAV_PREV_ICON : DEFAULT_ICON);
        builder.button(hasNext ? "Next" : "", FormImage.Type.PATH, hasNext ? NAV_NEXT_ICON : DEFAULT_ICON);
        // 2*SLOTS+2, +3: Buy / Sell tabs (active highlighted green, inactive gray).
        builder.button(sell ? "\u00A77Buy" : "\u00A7aBuy");
        builder.button(sell ? "\u00A7aSell" : "\u00A77Sell");

        builder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx < SLOTS) {
                sendItemGridForm(viewer, shop, p, sell);
            } else if (idx < 2 * SLOTS) {
                int item = idx - SLOTS;
                if (item < count) {
                    sendItemDetailForm(viewer, shop, pageItems.get(item), sell);
                }
            } else if (idx == 2 * SLOTS) {
                if (hasPrev) {
                    sendItemGridForm(viewer, shop, p - 1, sell);
                }
            } else if (idx == 2 * SLOTS + 1) {
                if (hasNext) {
                    sendItemGridForm(viewer, shop, p + 1, sell);
                }
            } else if (idx == 2 * SLOTS + 2) {
                sendItemGridForm(viewer, shop, 0, false);
            } else if (idx == 2 * SLOTS + 3) {
                sendItemGridForm(viewer, shop, 0, true);
            }
        });
        sendForm(viewer, builder.build());
    }

    // 3. Buy/Sell window: lore + quantity selection; submitting proceeds to confirmation.
    private static void sendItemDetailForm(Player viewer, Shop shop, ShopItem shopItem, boolean sell) {
        var itemStack = shopItem.getItemStack();
        boolean stackable = itemStack.getMaxStackSize() > 1;

        var builder = CustomForm.builder().title(SHOP_TOKEN + plainName(itemStack));
        builder.label(detailLabel(viewer, shopItem));
        if (stackable) {
            builder.slider("Quantity", 1, 64, 1, 1);
        }

        builder.validResultHandler(response -> {
            int quantity = stackable ? Math.max(1, Math.round(response.asSlider(1))) : 1;
            sendConfirmForm(viewer, shop, shopItem, !sell, quantity);
        });
        sendForm(viewer, builder.build());
    }

    // 4. Yes/No confirmation modal.
    private static void sendConfirmForm(Player viewer, Shop shop, ShopItem shopItem, boolean isBuy, int quantity) {
        var currency = shopItem.getCurrency().getCurrencyHandler();
        int unitPrice = isBuy ? shopItem.getBuyPrice() : shopItem.getSellPrice();
        int total = unitPrice * quantity;
        String content = "You are about to %s %sx %s for %s %s. Proceed?".formatted(
                isBuy ? "buy" : "sell", quantity, plainName(shopItem.getItemStack()), total, currencyGlyph(currency));
        var form = ModalForm.builder()
                .title(SHOP_TOKEN + (isBuy ? "Confirm Purchase" : "Confirm Sale"))
                .content(content)
                .button1("Yes")
                .button2("No")
                .validResultHandler(response -> {
                    if (response.clickedFirst()) {
                        performTransaction(viewer, shop, shopItem, isBuy, quantity);
                    } else {
                        sendItemDetailForm(viewer, shop, shopItem, !isBuy);
                    }
                })
                .build();
        sendForm(viewer, form);
    }

    /**
     * Runs the buy/sell on the main thread (inventory and currency mutations aren't safe from the
     * async form-response thread), then re-opens the item grid so the player can keep shopping.
     */
    private static void performTransaction(Player viewer, Shop shop, ShopItem shopItem, boolean isBuy, int quantity) {
        Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            var itemStack = shopItem.getItemStack();
            var itemName = itemStack.displayName();
            var transactionUtil = new ShopTransactionUtil(shopItem.getCurrency().getCurrencyHandler());
            if (isBuy) {
                var cost = shopItem.getBuyPrice() * quantity;
                transactionUtil.tryPurchase(viewer, cost, itemStack, itemName, quantity);
            } else {
                ShopLimitHandler sellLimitHandler = shopItem.hasSellLimit()
                        ? new ShopLimitHandler(viewer.getUniqueId(), shopItem.getId(), shopItem.getSellLimit())
                        : null;
                transactionUtil.sell(viewer, shopItem.getSellPrice(), itemStack, itemName, quantity, sellLimitHandler);
            }
            sendItemGridForm(viewer, shop, 0, !isBuy);
        });
    }

    /** Lore + balance + prices shown on the lore and buy-window screens. */
    private static String detailLabel(Player viewer, ShopItem shopItem) {
        var currency = shopItem.getCurrency().getCurrencyHandler();
        var label = new StringBuilder();
        List<Component> lore = shopItem.getItemStack().lore();
        if (lore != null) {
            lore.forEach(line -> label.append(plain(line)).append("\n"));
            if (!lore.isEmpty()) {
                label.append("\n");
            }
        }
        label.append("Balance: ").append(currency.getCurrency(viewer)).append(" ").append(currencyGlyph(currency)).append("\n");
        if (shopItem.getBuyPrice() != -1) {
            label.append("Buy: ").append(shopItem.getBuyPrice()).append(" ").append(currencyGlyph(currency)).append(" each\n");
        }
        if (shopItem.getSellPrice() != -1) {
            label.append("Sell: ").append(shopItem.getSellPrice()).append(" ").append(currencyGlyph(currency)).append(" each");
            if (shopItem.hasSellLimit()) {
                var sellLimitHandler = new ShopLimitHandler(viewer.getUniqueId(), shopItem.getId(), shopItem.getSellLimit());
                label.append(" (Stock: ").append(sellLimitHandler.getAmountLeft()).append(")");
            }
            label.append("\n");
        }
        return label.toString();
    }

    /**
     * Cell text: line 0 = buy price ($ acorns / @ gems) shown in the card; the remaining lines (name +
     * lore) are revealed by the pack's hover tooltip.
     */
    private static String gridButtonText(ShopItem shopItem, boolean sell) {
        var currency = shopItem.getCurrency().getCurrencyHandler();
        int price = sell ? shopItem.getSellPrice() : shopItem.getBuyPrice();
        // §c red in sell mode, §a green in buy mode (the glyph re-resets to white inside priceText)
        var sb = new StringBuilder(sell ? "\u00A7c" : "\u00A7a").append(priceText(currency, price));
        sb.append("\n").append(plainName(shopItem.getItemStack()));
        var lore = shopItem.getItemStack().lore();
        if (lore != null) {
            for (Component line : lore) {
                sb.append("\n").append(legacy(line));
            }
        }
        return sb.toString();
    }

    /** Cell price text: number then the currency glyph (forced white so the icon keeps its colours). */
    private static String priceText(Currency currency, int price) {
        return price + "\u00A7f" + currencyGlyph(currency);
    }

    /** Bedrock texture for a shop category, inferred from its name. */
    private static String categoryIcon(String shopName) {
        String n = plain(shopName).toLowerCase();
        String icon;
        if (n.contains("armor") || n.contains("tool")) icon = "tools_armors";
        else if (n.contains("crate")) icon = "crates";
        else if (n.contains("spawner")) icon = "spawners";
        else if (n.contains("drop")) icon = "mob_drops";
        else if (n.contains("mob")) icon = "spawn_eggs";
        else if (n.contains("ore") || n.contains("mineral")) icon = "minerals";
        else if (n.contains("rune") || n.contains("enchant")) icon = "enchants";
        else if (n.contains("farm")) icon = "farming";
        else if (n.contains("food")) icon = "foods";
        else if (n.contains("redstone")) icon = "redstone";
        else if (n.contains("block")) icon = "building_blocks";
        else icon = "miscellaneous";
        return "textures/gardens/shop/categories/icon_" + icon;
    }

    private static boolean isInMode(ShopItem shopItem, boolean sell) {
        var itemStack = shopItem.getItemStack();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        return sell ? shopItem.getSellPrice() != -1 : shopItem.getBuyPrice() != -1;
    }

    /**
     * Best-effort Bedrock texture path for an item's icon. Covers common vanilla materials; anything
     * unmapped falls back to the pack's {@link #DEFAULT_ICON}.
     */
    private static String itemIcon(ShopItem shopItem) {
        var stack = shopItem.getItemStack();
        if (stack == null) {
            return DEFAULT_ICON;
        }
        var path = ShopItemIcons.texturePath(stack.getType());
        return path != null ? path : DEFAULT_ICON;
    }

    private static String plainName(ItemStack itemStack) {
        var name = plain(itemStack.displayName());
        if (name.length() > 1 && name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }

    /** Currency glyph (acorn / gem) from the pack's font/glyph_E2 sheet. */
    private static String currencyGlyph(Currency currency) {
        return currency.name().equals("gems") ? GEM_GLYPH : ACORN_GLYPH;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Serialize with legacy section codes so MiniMessage/RGB colors survive into Bedrock text. */
    private static String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    private static String plain(String miniMessageOrPlain) {
        return PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(miniMessageOrPlain));
    }

    private static void sendForm(Player viewer, Form form) {
        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), form);
    }
}
