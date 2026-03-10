package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Collections;
import java.util.List;

public class WarpsMenu extends Menu {

    private static final String[] layout = new String[] {
            " abcdeij ",
            " wwwwwww ",
            " wwwwwww ",
            " wwwwwww ",
            "         ",
            " f  g  h "
    };

    private final List<WarpEntry> warps;
    private final boolean isYourWarps;

    public WarpsMenu(Player viewer, List<WarpEntry> warps, boolean isYourWarps, int highlightIdx) {
        super(layout, "<font:gardens:default>\u0103\u0128" + getHighlight(highlightIdx) + "</font>", viewer);
        this.warps = warps;
        this.isYourWarps = isYourWarps;
        initElements();
    }

    private static String getHighlight(int highlightIdx) {
        if (highlightIdx == -1) {
            return "";
        }
        return getShift(-151 + (18 * highlightIdx)) + "\u0129";
    }

    private void initElements() {
        var warpsGroup = new GuiElementGroup('w');
        warpsGroup.addElements(warps.stream().map(this::createWarpElement).toList());
        gui.addElement(warpsGroup);
        gui.addElements(List.of(
                createCategoryPicker('a', WarpEntry.Category.SHOP, 0),
                createCategoryPicker('b', WarpEntry.Category.FARM, 1),
                createCategoryPicker('c', WarpEntry.Category.MOB_FARM, 2),
                createCategoryPicker('d', WarpEntry.Category.BUILD, 3),
                createCategoryPicker('e', WarpEntry.Category.LANDMARK, 4),
                createCategoryPicker('i', WarpEntry.Category.GAME, 5),
                createCategoryPicker('j', WarpEntry.Category.MISC, 6)
        ));
        gui.addElement(createViewerWarps());
        gui.addElement(new GuiPageElement('f', blankItem, GuiPageElement.PageAction.PREVIOUS, "<blue>Previous"));
        gui.addElement(new GuiPageElement('h', blankItem, GuiPageElement.PageAction.NEXT, "<blue>Next"));
    }

    private GuiElement createWarpElement(WarpEntry warpEntry) {
        return new StaticGuiElement(
                'w',
                HeadUtil.createHead(warpEntry.getPlayerUUID()),
                click -> {
                    if (viewer.getUniqueId().equals(warpEntry.getPlayerUUID()) && click.getType() == ClickType.RIGHT) {
                        open(() -> new UpdateWarpMenu(viewer, warpEntry));
                    } else {
                        gui.close();
                        viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Warping to " + warpEntry.getName()));
                        WarpsHandler.INSTANCE.travel(viewer, warpEntry);
                    }
                    return true;
                },
                warpEntry.getName(),
                isYourWarps ? (warpEntry.isPublicAccess() ? "<green>Public" : "<red>Private") : "",
                warpEntry.getDescription() != null ? "<gray>" + warpEntry.getDescription() : "",
                !isYourWarps ? (warpEntry.isServerWarp() ? "<blue>Server Warp" : "<gray>by " + PlayerEntryCache.INSTANCE.getSync(warpEntry.getPlayerUUID()).getUsername()) : "",
                isYourWarps ? "<gray>" + warpEntry.getVisits() + " Unique Visits" : "",
                " ",
                "<white>괧 <gray>Warp",
                viewer.getUniqueId().equals(warpEntry.getPlayerUUID()) ? "<white>곘 <gray>Edit" : ""
        );
    }

    private GuiElement createCategoryPicker(char character, WarpEntry.Category category, int highlightIdx) {
        return new StaticGuiElement(
                character,
                GardensFurniture.itemRegistry.buildItem("warp_menu_icon_" + category.toString().toLowerCase()),
                click -> open(() -> new WarpsMenu(viewer, WarpsHandler.INSTANCE.getCachedWarps().warpsByCategory().getOrDefault(category, Collections.emptyList()), false, highlightIdx)),
                "<blue>" + category.getName()
        );
    }

    private GuiElement createViewerWarps() {
        return new StaticGuiElement(
                'g',
                HeadUtil.createHead(viewer),
                click -> open(() -> new WarpsMenu(viewer, WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(viewer.getUniqueId(), Collections.emptyList()), true, -1)),
                "<blue>Your Warps"
        );
    }
}
