package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.util.FontShift;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DepositGui implements InventoryHolder {

    private static final ItemStack blankHiddenItem = GardensFurniture.itemRegistry.buildItem("blank_hidden");
    private static final String title = "<white><font:gardens:default>" + FontShift.INSTANCE.getShift(-8) + "\u0136";
    public static final Set<Integer> ACORN_SLOTS = Set.of(
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24
    );
    private static final Map<Character, Character> NUMBER_MAPPINGS = Map.of(
            '0', '\uE157',
            '1', '\uE158',
            '2', '\uE159',
            '3', '\uE15A',
            '4', '\uE15B',
            '5', '\uE15C',
            '6', '\uE15D',
            '7', '\uE15E',
            '8', '\uE15F',
            '9', '\uE161'
    );

    private final Player viewer;
    private final Inventory inventory;

    public DepositGui(Player viewer) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 45, getTitle());
        // add filler slots
        IntStream.range(0, 11).forEach(i -> inventory.setItem(i, blankHiddenItem));
    }

    public Component getTitle() {
        var balance = (int) GardensPlayers.economy.getBalance(viewer);
        var numbersRemapped = String.valueOf(balance).chars()
                .mapToObj(c -> NUMBER_MAPPINGS.get((char) c).toString())
                .collect(Collectors.joining());
        return MiniMessage.miniMessage().deserialize(
                title + FontShift.INSTANCE.getShift(-115) + numbersRemapped
        );
    }

    public void depositItems() {
        ACORN_SLOTS.forEach(slot -> {
            var acorns = inventory.getItem(slot);
            if (isValidItem(acorns)) {
                inventory.setItem(slot, null);
                GardensPlayers.economy.depositPlayer(viewer, Objects.requireNonNull(acorns).getAmount());
            }
        });
        PlayerUtil.sendInventoryTitleChange(viewer.getOpenInventory(), getTitle());
    }

    public void open() {
        viewer.openInventory(getInventory());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public boolean isValidItem(ItemStack itemStack) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        return customItem != null && customItem.getIdentifier().equals("golden_acorn");
    }
}
