package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.util.BedrockUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class BackpackGui implements InventoryHolder {

    private final Player viewer;
    @Getter
    private final ItemStack backpackItem;
    private final Inventory inventory;
    private final ItemStack blankHiddenItem = GardensFurniture.itemRegistry.buildItem("blank_hidden");
    @Getter
    private final int fillerSlots;
    @Getter
    private final boolean isWardrobeBackpack;
    private boolean closed;
    // stored items that didn't fit (eg contents from a bigger container); the opener hands them back
    @Getter
    private final List<ItemStack> overflow = new ArrayList<>();

    public BackpackGui(Player viewer, RowConfig rowConfig, ItemStack backpackItem, ItemStack[] items, boolean isWardrobeBackpack) {
        this.viewer = viewer;
        var isBedrockViewer = BedrockUtil.isBedrock(viewer);
        this.fillerSlots = isBedrockViewer ? 0 : rowConfig.getFillerRows() * 9;
        this.backpackItem = backpackItem;
        var title = isBedrockViewer ? Component.text("Backpack") : getTitle(rowConfig);
        // bedrock has no filler rows, so the inventory is just the backpack rows
        var totalRows = isBedrockViewer ? rowConfig.getRows() : rowConfig.getTotalRows();
        this.inventory = Bukkit.createInventory(this, totalRows * 9, title);
        this.isWardrobeBackpack = isWardrobeBackpack;
        addEmptySlots();
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                var item = items[i];
                if (fillerSlots + i < inventory.getSize()) {
                    inventory.setItem(fillerSlots + i, item);
                } else if (item != null && !item.getType().isAir()) {
                    overflow.add(item);
                }
            }
            // anything past the end goes into free slots first
            overflow.removeIf(item -> inventory.addItem(item).isEmpty());
        }
    }

    private static Component getTitle(RowConfig rowConfig) {
        return MiniMessage.miniMessage().deserialize("<white><font:gardens:default>\u0105\u010B" + rowConfig.getSymbol() + "</font>");
    }

    private void addEmptySlots() {
        IntStream.range(0, fillerSlots).forEach(i -> inventory.setItem(i, blankHiddenItem));
    }

    public void open() {
        viewer.openInventory(getInventory());
    }

    public ItemStack[] getItemContents() {
        // remove filler content
        var originalContents = inventory.getContents();
        if (fillerSlots == 0) {
            return originalContents;
        }
        return Arrays.copyOfRange(originalContents, fillerSlots, originalContents.length);
    }

    /**
     * Marks this gui as closed, returning false if it already was. Closing can be handled twice
     * (manual close followed by the InventoryCloseEvent), so this keeps the save/give-back to once.
     */
    public boolean markClosed() {
        if (closed) {
            return false;
        }
        closed = true;
        return true;
    }

    /**
     * Removes and returns every item in the storage slots (filler excluded) that matches the predicate.
     */
    public List<ItemStack> removeItems(Predicate<ItemStack> predicate) {
        var removed = new ArrayList<ItemStack>();
        for (int i = fillerSlots; i < inventory.getSize(); i++) {
            var item = inventory.getItem(i);
            if (item != null && !item.getType().isAir() && predicate.test(item)) {
                removed.add(item);
                inventory.setItem(i, null);
            }
        }
        return removed;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @RequiredArgsConstructor
    @AllArgsConstructor
    @Getter
    public enum RowConfig {
        TIER1(1, '\u0130', 5),
        TIER2(2, '\u0131', 4),
        TIER3(3, '\u0132', 3),
        TIER4(4, '\u0133', 2),
        TIER5(5, '\u0134', 1),
        TIER6(6, '\u0135');

        private final int rows;
        private final char symbol;
        private int fillerRows;

        public int getTotalRows() {
            return rows + fillerRows;
        }

        public static RowConfig fromRows(int rows) {
            return Arrays.stream(RowConfig.values()).
                    filter(rowConfig -> rowConfig.rows == rows)
                    .findFirst()
                    .orElseThrow();
        }
    }
}
