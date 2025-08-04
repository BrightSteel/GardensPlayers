package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.util.HeadUtil;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public abstract class WarpMenu extends Menu {

    private static final String[] layout = new String[] {
            "         ",
            "  a b c  ",
            "         ",
            " f ggg e "
    };
    private static final List<WarpEntry.Category> categories = List.of(WarpEntry.Category.values());

    protected String warpName;
    protected boolean isPublic;
    protected String description;
    private int categoryIndex;
    protected final String updateText;

    public WarpMenu(Player viewer, String warpName, boolean isPublic, WarpEntry.Category category,
                    String updateText, String title, String description) {
        super(layout, title, viewer);
        this.warpName = warpName;
        this.isPublic = isPublic;
        this.categoryIndex = categories.indexOf(category);
        this.updateText = updateText;
        this.description = description;
        initElements();
    }

    private void initElements() {
        gui.addElements(List.of(
                createMainElement(),
                createCategoryElement(),
                createLockedElement(),
                createCloseButton(),
                createCreateButton(),
                createSetDescriptionElement()
        ));
    }

    private GuiElement createMainElement() {
        return new DynamicGuiElement('b', () -> new StaticGuiElement(
                'b',
                HeadUtil.createHead(viewer),
                click -> {
                    gui.close();
                    viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Please type a warp name"));
                    OnlinePlayerCache.INSTANCE.getOnlinePlayerData(viewer.getUniqueId()).setTextCapture(text -> {
                        this.warpName = text;
                        this.open();
                        gui.draw();
                    });
                    return true;
                },
                "<blue>Warp Name",
                "<gold>" + warpName,
                " ",
                "<white>괧 <gray>Change"
        ));
    }

    private GuiElement createSetDescriptionElement() {
        return new DynamicGuiElement('e', () -> new StaticGuiElement(
                'e',
                GardensFurniture.itemRegistry.buildItem("warp_icon_description"),
                click -> {
                    gui.close();
                    viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Please type a description"));
                    OnlinePlayerCache.INSTANCE.getOnlinePlayerData(viewer.getUniqueId()).setTextCapture(text -> {
                        this.description = text;
                        this.open();
                        gui.draw();
                    });
                    return true;
                },
                "<blue>Description",
                "<gray>" + (description == null ? "None" : description),
                " ",
                "<white>괧 <gray>Change"
        ));
    }

    private GuiElement createLockedElement() {
        return new DynamicGuiElement('a', () -> new StaticGuiElement(
                'a',
                GardensFurniture.itemRegistry.buildItem(isPublic ? "warp_icon_public" : "warp_icon_private"),
                click -> {
                    isPublic = !isPublic;
                    gui.draw();
                    return true;
                },
                "<blue>Warp Visibility",
                isPublic ? "<green>Public" : "<red>Private",
                " ",
                "<white>괧 <gray>Toggle"
        ));
    }

    protected WarpEntry.Category getCategory() {
        return categories.get(categoryIndex);
    }

    private GuiElement createCategoryElement() {
        return new DynamicGuiElement('c', () -> new StaticGuiElement(
                'c',
                GardensFurniture.itemRegistry.buildItem("warp_icon_" + getCategory().toString().toLowerCase()),
                click -> {
                    if (click.getType() == ClickType.LEFT) {
                        if (categoryIndex == categories.size() - 1) {
                            categoryIndex = 0;
                        } else {
                            categoryIndex++;
                        }
                    } else if (click.getType() == ClickType.RIGHT) {
                        if (categoryIndex == 0) {
                            categoryIndex = categories.size() - 1;
                        } else {
                            categoryIndex--;
                        }
                    }
                    gui.draw();
                    return true;
                },
                "<blue>Warp Category",
                "<gold>" + getCategory().getName(),
                " ",
                "<white>괧 <gray>Next",
                "<white>곘 <gray>Prev"
        ));
    }

    private GuiElement createCloseButton() {
        return new StaticGuiElement(
                'f',
                blankItem,
                click -> {
                    gui.close();
                    return true;
                },
                "<red>Cancel"
        );
    }

    protected abstract void onUpdate();

    private GuiElement createCreateButton() {
        return new StaticGuiElement(
                'g',
                blankItem,
                click -> {
                    gui.close();
                    onUpdate();
                    return true;
                },
                updateText
        );
    }
}
