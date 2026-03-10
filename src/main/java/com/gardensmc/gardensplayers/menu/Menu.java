package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.util.FontShift;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public class Menu {

    protected Player viewer;

    protected InventoryGui gui;
    protected ItemStack blankItem, blankHiddenItem;

    public Menu(String[] layout, String title, Player viewer) {
        this(layout, MiniMessage.miniMessage().deserialize("<white>" + title), viewer);
    }

    public Menu(String[] layout, Component title, Player viewer) {
        this.viewer = viewer;
        this.gui = new InventoryGui(
                GardensFurniture.plugin,
                new InventoryGui.InventoryCreator(
                        (gui, who, type) -> GardensFurniture.plugin.getServer().createInventory(new InventoryGui.Holder(gui), type, title),
                        (gui, who, size) -> GardensFurniture.plugin.getServer().createInventory(new InventoryGui.Holder(gui), size, title)
                ),
                (meta, s) -> meta.displayName(toComponent(s)),
                (meta, strings) -> meta.lore(strings.stream().map(this::toComponent).toList()),
                viewer,
                "",
                layout
        );
        this.blankItem = GardensFurniture.itemRegistry.buildItem("blank");
        this.blankHiddenItem = GardensFurniture.itemRegistry.buildItem("blank_hidden");
        initializeElements();
    }

    protected final Component toComponent(String s) {
        return MiniMessage.miniMessage().deserialize("<!italic><white>" + s + "</white>");
    }

    protected final DynamicGuiElement buildBackButton(Supplier<Menu> priorMenu, String priorButtonText) {
        return new DynamicGuiElement('p', () -> new StaticGuiElement('p', GardensFurniture.itemRegistry.buildItem("back_button"), click -> {
            if (gui.getPageNumber(viewer) > 0) {
                gui.setPageNumber(viewer, gui.getPageNumber(viewer) - 1);
                gui.draw();
            } else {
                open(priorMenu);
            }
            return true;
        }, gui.getPageNumber(viewer) > 0 ? "<gold>Back" : "<gold>" + priorButtonText));
    }

    protected final DynamicGuiElement buildNextButton() {
        return new DynamicGuiElement('n', () -> gui.getPageNumber(viewer) == 0 || (gui.getPageNumber(viewer) + 1 < gui.getPageAmount(viewer))
                ? new StaticGuiElement('n',  GardensFurniture.itemRegistry.buildItem("next_button"), click -> {
                    if (gui.getPageNumber(viewer) + 1 < gui.getPageAmount(viewer)) {
                        gui.setPageNumber(viewer, gui.getPageNumber(viewer) + 1);
                        gui.draw();
                    }
                    return true;
                }, "<gold>Next")
                : null
        );
    }

    protected void initializeElements() {
    }

    public void open() { gui.show(viewer); }

    public boolean open(Supplier<Menu> menuSupplier) {
        InventoryGui.clearHistory(viewer);
        menuSupplier.get().open();
        return true;
    }

    protected static String getShift(int shift) {
        return FontShift.INSTANCE.getShift(shift);
    }

}