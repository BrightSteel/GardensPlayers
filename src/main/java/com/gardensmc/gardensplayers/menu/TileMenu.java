package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import com.gardensmc.gardensplayers.menu.tile.TileItem;
import com.gardensmc.gardensplayers.util.FontShift;
import com.gardensmc.gardensplayers.util.MessageUtil;
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

    protected static final String[] DEFAULT_LAYOUT = new String[] {
            "a  ggghhh",
            "bbbbbbbbb",
            "ddddddddd",
            "ddddddddd",
            "ddddddddd",
            "eee   fff"
    };

    private static final char CURRENT_TAB = '\u0139';
    private static final char TAB = '\u013A';
    /** The tabs at either end of the category row when there are more categories that way (the pack's shop_ui arrows). */
    private static final char PREV_CATEGORIES = '\uE300', NEXT_CATEGORIES = '\uE301';
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCompactNumberInstance(
            Locale.US, NumberFormat.Style.SHORT);

    private final String[] layout;
    private final CategoryPages categoryPages;
    private final int page;
    private final int selectedCategory;
    private final int guiPageNumber;
    private final boolean showBalances;
    private final char bg;
    private final String extraTitleChars;
    private int guiMaxPages;
    /** What the back button (the top left, 'a') does; none when null (the menu has no back button). */
    private java.util.function.Consumer<TileMenu> back;

    protected TileMenu(String[] layout, Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg, String extraTitleChars) {
        super(layout, buildTitle(viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg, extraTitleChars), viewer);
        this.layout = layout;
        this.categoryPages = pages;
        this.page = page;
        this.selectedCategory = selectedCategory;
        this.guiPageNumber = guiPageNumber;
        this.showBalances = showBalances;
        this.bg = bg;
        this.extraTitleChars = extraTitleChars;
        initElements();
    }

    public static Builder builder(Player viewer, CategoryPages pages, char bg) {
        return new Builder(viewer, pages, bg);
    }

    /**
     * Constructs a menu of this concrete type. Subclasses override this so that navigation (paging, tab switches,
     * {@link #reopenWith}) recreates the subtype rather than degrading to a plain {@link TileMenu}.
     */
    protected TileMenu create(String[] layout, Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg, String extraTitleChars) {
        return new TileMenu(layout, viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg, extraTitleChars);
    }

    /** Re-renders this menu with the given navigation state, carrying over its layout, balances and title. */
    private TileMenu copyWith(CategoryPages pages, int page, int selectedCategory, int guiPageNumber) {
        var copy = create(layout, viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg, extraTitleChars);
        copy.setBack(back);
        return copy;
    }

    /** Gives this menu a back button (the top left, 'a'), doing this; none when null. */
    public void setBack(java.util.function.Consumer<TileMenu> back) {
        this.back = back;
        gui.removeElement('a');
        if (back != null) {
            gui.addElement(new StaticGuiElement('a', blankItem, click -> {
                this.back.accept(this);
                return true;
            }, "<white>Back"));
        }
    }

    /** Closes it (for a back button). */
    public void close() {
        gui.close();
    }

    private static String buildTitle(Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg, String extraTitleChars) {
        var categoryPage = pages.getPage(page);
        var currCategory = categoryPage.getCategories().get(selectedCategory);

        var titleBuilder = new StringBuilder("<white><font:gardens:default>" + getShift(-8) + bg);
        titleBuilder.append(getShift(-256));
        titleBuilder.append(getShift(79));

        if (!extraTitleChars.isEmpty()) {
            titleBuilder.append(extraTitleChars);
        }

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
        var pageNumbers = "%s / %s".formatted(guiPageNumber + 1, (int) Math.ceil((double) Math.max(currCategory.getItems().size(), 1) / 27));
        titleBuilder.append("<font:gardens:tile_page>").append(pageNumbers).append("</font>");

        // shift back
        titleBuilder.append(getShift(-FontShift.INSTANCE.getStringWidth(pageNumbers)));
        titleBuilder.append(getShift(-66));

        // assemble category backgrounds
        int categoryWidth = 0;
        if (page > 0) {
            titleBuilder.append(PREV_CATEGORIES).append(getShift(-1));
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
        if (categoryPage.isHasNextPage()) {
            titleBuilder.append(NEXT_CATEGORIES).append(getShift(-1));
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

    protected void initElements() {
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
                        open(() -> copyWith(categoryPages, prevPage,
                                categoryPages.getPage(prevPage).getCategories().size() - 1, 0));
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
                                open(() -> copyWith(categoryPages, page, finalI, 0));
                            }
                            return true;
                        },
                        tabText(category))
                );
        }
        if (categoryPage.isHasNextPage()) {
            categoryGrp.addElement(new StaticGuiElement(
                    'b',
                    blankItem,
                    click -> {
                        open(() -> copyWith(categoryPages, page + 1, 0, 0));
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
                open(() -> copyWith(categoryPages, page, selectedCategory, guiPageNumber - 1));
            }
            return true;
        }, "Back")));
        gui.addElement(new DynamicGuiElement('f', () -> new StaticGuiElement('f', guiPageNumber == guiMaxPages - 1 ? null : blankItem, click -> {
            if (guiPageNumber < guiMaxPages - 1) {
                open(() -> copyWith(categoryPages, page, selectedCategory, guiPageNumber + 1));
            }
            return true;
        }, "Next")));
    }

    // opens a new menu with same page, etc; mainly for opening with re-built title
    public void reopen() {
        copyWith(categoryPages, page, selectedCategory, guiPageNumber).open();
    }

    // re-renders with freshly-built categories, preserving the open tab and page
    public void reopenWith(CategoryPages newCategoryPages) {
        copyWith(newCategoryPages, page, selectedCategory, guiPageNumber).open();
    }

    /**
     * Fluent builder for a {@link TileMenu}. {@code viewer}, {@code pages} and {@code bg} are required; {@code page},
     * {@code selectedCategory} and {@code guiPageNumber} default to 0 and {@code showBalances} to {@code false}.
     */
    public static final class Builder {

        private final Player viewer;
        private final CategoryPages pages;
        private final char bg;
        private String[] layout = DEFAULT_LAYOUT;
        private java.util.function.Consumer<TileMenu> back;
        private int page;
        private int selectedCategory;
        private int guiPageNumber;
        private boolean showBalances;
        private String extraTitleChars = "";

        private Builder(Player viewer, CategoryPages pages, char bg) {
            this.viewer = viewer;
            this.pages = pages;
            this.bg = bg;
        }

        public Builder layout(String[] layout) {
            this.layout = layout;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder selectedCategory(int selectedCategory) {
            this.selectedCategory = selectedCategory;
            return this;
        }

        public Builder guiPageNumber(int guiPageNumber) {
            this.guiPageNumber = guiPageNumber;
            return this;
        }

        public Builder showBalances(boolean showBalances) {
            this.showBalances = showBalances;
            return this;
        }

        public Builder extraTitleChars(String extraTitleChars) {
            this.extraTitleChars = extraTitleChars;
            return this;
        }

        public TileMenu build() {
            var menu = new TileMenu(layout, viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg, extraTitleChars);
            menu.setBack(back);
            return menu;
        }

        /** A back button (the top left), doing this. */
        public Builder back(java.util.function.Consumer<TileMenu> back) {
            this.back = back;
            return this;
        }

        public void open() {
            build().open();
        }
    }


    /** A tab's tooltip: its name, then its lore lines, MiniMessage as the menu reads every line (not italic, white). */
    private static String[] tabText(com.gardensmc.gardensplayers.menu.tile.TileCategory category) {
        var lines = new java.util.ArrayList<String>();
        lines.add(category.getDisplayName());
        lines.addAll(category.getLore());
        return lines.toArray(String[]::new);
    }

}
