package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import com.gardensmc.gardensplayers.menu.tile.TileItem;
import com.gardensmc.gardensplayers.util.FontShift;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TileMenu extends Menu {

    private static final String[] layout = new String[] {
            "a  ggghhh",
            "bbbbbbbbb",
            "ddddddddd",
            "ddddddddd",
            "ddddddddd",
            "eee   fff"
    };

    private static final char CURRENT_TAB = '\u0139';
    private static final char TAB = '\u013A';
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCompactNumberInstance(
            Locale.US, NumberFormat.Style.SHORT);

    private final CategoryPages categoryPages;
    private final int page;
    private final int selectedCategory;
    private final int guiPageNumber;
    private final boolean showBalances;
    private final char bg;
    private int guiMaxPages;

    public TileMenu(Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg) {
        super(layout, buildTitle(viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg), viewer);
        this.categoryPages = pages;
        this.page = page;
        this.selectedCategory = selectedCategory;
        this.guiPageNumber = guiPageNumber;
        this.showBalances = showBalances;
        this.bg = bg;
        initElements();
    }

    public TileMenu(Player viewer, CategoryPages pages, int page, boolean showBalances, char bg) {
        this(viewer, pages, page, 0, 0, showBalances, bg);
    }

    public TileMenu(Player viewer, CategoryPages pages, boolean showBalances, char bg) {
        this(viewer, pages, 0, 0, 0, showBalances, bg);
    }

    private static String buildTitle(Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg) {
        var categoryPage = pages.getPage(page);
        var currCategory = categoryPage.getCategories().get(selectedCategory);

        var titleBuilder = new StringBuilder("<white><font:gardens:default>" + getShift(-8) + bg);
        titleBuilder.append(getShift(-256));
        titleBuilder.append(getShift(79));

        if (showBalances) {
            // append balances
            var gemBalance = CURRENCY_FORMATTER.format(GemsCurrencyHandler.INSTANCE.getCurrency(viewer));
            titleBuilder.append("<font:gardens:tile_upper>").append(gemBalance).append("</font>");
            var gemWidth = FontShift.INSTANCE.getStringWidth(gemBalance);

            // some width between
            var balanceSpace = 53 - gemWidth;
            titleBuilder.append(getShift(balanceSpace));

            var acornBalance = CURRENCY_FORMATTER.format(AcornsCurrencyHandler.INSTANCE.getCurrency(viewer));
            titleBuilder.append("<font:gardens:tile_upper>").append(acornBalance).append("</font>");
            var acornWidth = FontShift.INSTANCE.getStringWidth(acornBalance);

            // shift backwards
            titleBuilder.append(getShift(-(gemWidth + balanceSpace + acornWidth)));
        }

        // append page numbers
        titleBuilder.append(getShift(-1));
        var pageNumbers = "%s / %s".formatted(guiPageNumber + 1, (int) Math.ceil((double) currCategory.getItems().size() / 27));
        titleBuilder.append("<font:gardens:tile_page>").append(pageNumbers).append("</font>");

        // shift back
        titleBuilder.append(getShift(-FontShift.INSTANCE.getStringWidth(pageNumbers)));
        titleBuilder.append(getShift(-66));

        // assemble category backgrounds
        int categoryWidth = 0;
        if (page > 0) {
            titleBuilder.append(getShift(18));
            categoryWidth += 18;
        }

        for (int i = 0; i < categoryPage.getCategories().size(); i++) {
            if (categoryPage.getCategories().get(i).equals(currCategory)) {
                titleBuilder.append(CURRENT_TAB);
            } else {
                titleBuilder.append(TAB);
            }
            titleBuilder.append(getShift(-1));
            categoryWidth += 18;
        }
        titleBuilder.append(getShift(-categoryWidth));

        // assemble row backgrounds
        var items = currCategory.getItems().subList(guiPageNumber * 27, currCategory.getItems().size());
        var idx = 0;
        for (int i = 0; i < 3; i++) {
            if (idx == items.size()) break;
            idx = assembleRowTiles(titleBuilder, viewer, i, items, idx, items.size());
            if (idx < items.size()) {
                // offset back for next row
                titleBuilder.append(getShift(-162));
            }
        }
        return titleBuilder.toString();
    }

    // returns modified items left
    private static int assembleRowTiles(StringBuilder titleBuilder, Player viewer, int row, List<? extends TileItem> items, int idx, int maxItems) {
        for (int i = 0; i < 9; i++) {
            if (idx == maxItems) break;
            var tileItem = items.get(idx);
            if (tileItem.isSpacer()) {
                titleBuilder.append(getShift(18));
            } else {
                titleBuilder.append(tileItem.getTileColor(viewer).getCharacters()[row]).append(getShift(-1));
            }
            idx++;
        }
        return idx;
    }

    private void initElements() {
        var categoryGrp = new GuiElementGroup('b');
        if (showBalances) {
            gui.addElement(new StaticGuiElement('g', blankItem, "<blue>Gems", "<gray>Earn gems with /vote or /buy"));
            gui.addElement(new StaticGuiElement('h', blankItem, "<gold>Acorns", "<gray>Earn acorns with /vote or selling"));
        }

        if (page > 0) {
            var prevPage = page - 1;
            categoryGrp.addElement(new StaticGuiElement(
                    'b',
                    blankItem,
                    click -> {
                        open(() -> new TileMenu(viewer, categoryPages, prevPage, categoryPages.getPage(prevPage).getCategories().size() - 1, 0, showBalances, bg));
                        return true;
                    },
                    "Back"
            ));
        }
        var categoryPage = categoryPages.getPage(page);
        var currCategory = categoryPage.getCategories().get(selectedCategory);
        for (int i = 0; i < categoryPage.getCategories().size(); i++) {
            var category = categoryPage.getCategories().get(i);
            int finalI = i;
            var icon = GardensFurniture.itemRegistry.buildItem(category.getIconId());
            categoryGrp.addElement(new StaticGuiElement(
                        'b',
                        icon != null ? icon : new ItemStack(Material.PAPER),
                        click -> {
                            if (!category.equals(currCategory)) {
                                open(() -> new TileMenu(viewer, categoryPages, page, finalI, 0, showBalances, bg));
                            }
                            return true;
                        },
                        category.getDisplayName())
                );
        }
        if (categoryPage.isHasNextPage()) {
            categoryGrp.addElement(new StaticGuiElement(
                    'b',
                    blankItem,
                    click -> {
                        open(() -> new TileMenu(viewer, categoryPages, page + 1, 0, 0, showBalances, bg));
                        return true;
                    },
                    "Next"
            ));
        }
        gui.addElement(categoryGrp);

        // elements
        guiMaxPages = (int) Math.ceil((double) currCategory.getItems().size() / 27);
        var contentGrp = new GuiElementGroup('d');
        contentGrp.addElements(currCategory.getItems().subList(guiPageNumber * 27, currCategory.getItems().size()).stream().map(tileItem -> new StaticGuiElement(
                'd',
                tileItem.getItemStack(viewer),
                click -> {
                    tileItem.onClick(click, this);
                    return true;
                }))
                .collect(Collectors.toList()));
        gui.addElement(contentGrp);

        // pagination
        gui.addElement(new DynamicGuiElement('e', () -> new StaticGuiElement('e', guiPageNumber == 0 ? null : blankItem, click -> {
            if (guiPageNumber > 0) {
                open(() -> new TileMenu(viewer, categoryPages, page, selectedCategory, guiPageNumber - 1, showBalances, bg));
            }
            return true;
        }, "Back")));
        gui.addElement(new DynamicGuiElement('f', () -> new StaticGuiElement('f', guiPageNumber == guiMaxPages - 1 ? null : blankItem, click -> {
            if (guiPageNumber < guiMaxPages - 1) {
                open(() -> new TileMenu(viewer, categoryPages, page, selectedCategory, guiPageNumber + 1, showBalances, bg));
            }
            return true;
        }, "Next")));
    }

    // opens a new TileMenu with same page, etc; mainly for opening with re-built title
    public void reopen() {
        new TileMenu(viewer, categoryPages, page, selectedCategory, guiPageNumber, showBalances, bg).open();
    }

}
