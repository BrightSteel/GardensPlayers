package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.util.FontShift;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.Worlds;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Home;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * /homes: the player's HuskHomes homes, as a popup over their profile (dimmed), in the style of the crates menu. Each
 * page shows 21: their homes, then a slot for each home they can still set, then locked slots.
 * The pack's gardens:homes font draws it all (the profile, the popup, its close button and page bar); the items sit
 * in the popup's slots.
 */
public class HomesMenu extends Menu {

    // 5 rows, as /profile, with the crates menu's contents popup over it: its title and close X in row 0 (the X on
    // column 7's slot), its homes in rows 1 to 3 (7 across), its page row in row 4 (the buttons two slots wide)
    private static final String[] LAYOUT = new String[] {
            "z      xz",
            "zabcdefgz",
            "zhijklmnz",
            "zopqrstuz",
            "z<<   >>z"
    };
    private static final char[] SLOTS = "abcdefghijklmnopqrstu".toCharArray();
    private static final int PER_PAGE = SLOTS.length;
    /** Ranks with a great many homes: more add slots than this aren't all shown. */
    private static final int MAX_ADD_SHOWN = 45;

    private static final Key HEAD_MODEL = Key.key("gardens", "homes/head");
    private static final Key ADD_MODEL = Key.key("gardens", "homes/add");
    private static final Key LOCKED_MODEL = Key.key("gardens", "homes/locked");
    private static final DateTimeFormatter CREATED = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
            .withZone(ZoneId.systemDefault());
    private static final String LEFT = "<white>괧 <gray>", RIGHT = "<white>곘 <gray>";

    // where things are on the background, in GUI pixels: "Homes" at 26; the page bar from 62 to 113 (the close X is
    // drawn on it, over column 7's slot in row 0)
    private static final int TITLE_X = 26, BAR_X = 62, BAR_WIDTH = 52;

    private final List<Home> homes;
    private final int maxHomes;
    private final int page;
    /** Homes right-clicked once: a second right-click deletes them. */
    private final Set<String> confirmingDelete = new HashSet<>();

    private HomesMenu(Player viewer, List<Home> homes, int maxHomes, int page, int pages) {
        super(LAYOUT, MiniMessage.miniMessage().deserialize(title(page, pages)), viewer);
        this.homes = homes;
        this.maxHomes = maxHomes;
        this.page = page;
        addElements(pages);
    }

    /** Opens the viewer's homes at a page, once HuskHomes has them. */
    public static void open(Player viewer, int page) {
        var api = HuskHomesAPI.getInstance();
        var user = api.adaptUser(viewer);
        int maxHomes = api.getMaxHomeSlots(user);
        api.getUserHomes(user).thenAccept(homes -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            if (!viewer.isOnline()) return;
            var list = new ArrayList<>(homes);
            int pages = pages(list.size(), maxHomes);
            // over whatever's open (the profile, or this on another page): no history to go back to
            InventoryGui.clearHistory(viewer);
            new HomesMenu(viewer, list, maxHomes, Math.clamp(page, 0, pages - 1), pages).open();
        }));
    }

    // the pages its homes and add slots take; a rank with a great many homes isn't shown pages of add slots
    private static int pages(int homes, int maxHomes) {
        int entries = Math.min(Math.max(homes, maxHomes), homes + MAX_ADD_SHOWN);
        return Math.max(1, (entries + PER_PAGE - 1) / PER_PAGE);
    }

    // the background (gardens:homes , from the chest's left edge), "Homes" in the popup, and the page number
    // centred in its bar
    private static String title(int page, int pages) {
        var number = (page + 1) + " / " + pages;
        int afterTitle = TITLE_X + FontShift.INSTANCE.getStringWidth("Homes");
        int numberAt = BAR_X + (BAR_WIDTH - (FontShift.INSTANCE.getStringWidth(number) - 1)) / 2;
        return "<white><font:gardens:homes></font></white>"
                + "<#494949><font:gardens:homes_title>Homes</font></#494949>"
                + "<font:gardens:default>" + getShift(numberAt - afterTitle) + "</font>"
                + "<white><font:gardens:homes_page>" + number + "</font></white>";
    }

    private void addElements(int pages) {
        // the close X, or anywhere outside the popup, closes it: back to the profile behind it
        gui.addElement(new StaticGuiElement('x', blankItem, click -> {
            backToProfile();
            return true;
        }, "<white>Close"));
        gui.addElement(new StaticGuiElement('z', blankHiddenItem, click -> {
            backToProfile();
            return true;
        }));
        gui.addElement(new StaticGuiElement('<', page > 0 ? blankItem : blankHiddenItem, click -> {
            if (page > 0) open(viewer, page - 1);
            return true;
        }, "<white>Previous Page"));
        gui.addElement(new StaticGuiElement('>', page + 1 < pages ? blankItem : blankHiddenItem, click -> {
            if (page + 1 < pages) open(viewer, page + 1);
            return true;
        }, "<white>Next Page"));

        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            char slot = SLOTS[i];
            if (index < homes.size()) {
                gui.addElement(homeElement(slot, homes.get(index)));
            } else if (index < Math.min(maxHomes, homes.size() + MAX_ADD_SHOWN)) {
                gui.addElement(addElement(slot));
            } else {
                gui.addElement(new StaticGuiElement(slot, item(LOCKED_MODEL), click -> true,
                        "<red>Locked", "<gray>Higher ranks can set more homes"));
            }
        }
    }

    private GuiElement homeElement(char slot, Home home) {
        var name = home.getName();
        return new DynamicGuiElement(slot, () -> {
            boolean confirming = confirmingDelete.contains(name);
            var icon = HeadUtil.createHead(viewer);
            icon.setData(DataComponentTypes.ITEM_MODEL, HEAD_MODEL);
            return new StaticGuiElement(slot, icon, click -> {
                if (click.getType() == ClickType.RIGHT) {
                    if (confirmingDelete.add(name)) {
                        gui.draw();
                    } else {
                        viewer.performCommand("huskhomes:delhome " + name);
                        homes.remove(home);
                        int pages = pages(homes.size(), maxHomes);
                        InventoryGui.clearHistory(viewer);
                        new HomesMenu(viewer, homes, maxHomes, Math.min(page, pages - 1), pages).open();
                    }
                    return true;
                }
                if (click.getType() == ClickType.LEFT) {
                    gui.close();
                    viewer.performCommand("huskhomes:home " + name);
                }
                return true;
            },
                    "<white>" + name,
                    "<gray>" + Worlds.displayName(home.getWorld().getName()) + " at " + (int) Math.floor(home.getX())
                            + ", " + (int) Math.floor(home.getY()) + ", " + (int) Math.floor(home.getZ()),
                    "<dark_gray>Set " + CREATED.format(home.getMeta().getCreationTime()),
                    " ",
                    LEFT + "Go here",
                    confirming ? "<white>곘 <red>Again to remove it" : RIGHT + "Remove");
        });
    }

    private GuiElement addElement(char slot) {
        return new StaticGuiElement(slot, item(ADD_MODEL), click -> {
            gui.close();
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Type a name for your new home"));
            OnlinePlayerCache.INSTANCE.getOnlinePlayerData(viewer.getUniqueId()).setTextCapture(text -> {
                var name = text.trim().replace(' ', '_');
                if (name.isEmpty()) return;
                viewer.performCommand("huskhomes:sethome " + name);
                // HuskHomes saves it in a moment: then show it
                Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> {
                    if (viewer.isOnline()) open(viewer, page);
                }, 10);
            });
            return true;
        },
                "<white>New Home",
                "<gray>Saves the spot you're standing on",
                " ",
                LEFT + "Set it");
    }

    private static ItemStack item(Key model) {
        var item = new ItemStack(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, model);
        return item;
    }
}
