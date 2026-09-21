package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.GuildCache;
import com.gardensmc.gardensplayers.database.entry.GuildOwnerEntry;
import com.gardensmc.gardensplayers.guild.GuildsHandler;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays all guilds as a paginated grid of their owners' heads.
 */
public class GuildsMenu extends Menu {

    private static final int DESCRIPTION_LINE_LENGTH = 20;

    private static final String[] LAYOUT = new String[]{
            "a      bc",
            "ddddddddd",
            "ddddddddd",
            "ddddddddd",
            "ddddddddd",
            "eee   fff"
    };

    private final List<GuildOwnerEntry> guilds;
    private final boolean viewerInGuild;

    public GuildsMenu(Player viewer, List<GuildOwnerEntry> guilds, boolean viewerInGuild) {
        super(LAYOUT, "<font:gardens:default>" + getShift(-8) + "\u015E</font>", viewer);
        this.guilds = guilds;
        this.viewerInGuild = viewerInGuild;
        initElements();
    }

    private void initElements() {
        var guildsGroup = new GuiElementGroup('d');
        guildsGroup.addElements(guilds.stream().map(this::buildGuildElement).toList());
        gui.addElement(guildsGroup);

        gui.addElement(new GuiPageElement('e', blankItem, GuiPageElement.PageAction.PREVIOUS, "Back"));
        gui.addElement(new GuiPageElement('f', blankItem, GuiPageElement.PageAction.NEXT, "Next"));
    }

    private GuiElement buildGuildElement(GuildOwnerEntry entry) {
        var guild = entry.getGuild();
        var online = Bukkit.getPlayer(entry.getOwnerUUID());
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(entry.getOwnerUUID());

        var text = new ArrayList<String>();
        text.add("<yellow>" + guild.getName());
        text.add("<gray>Owner: <white>" + entry.getOwnerUsername());
        if (guild.getDescription() != null) {
            // wrap onto multiple lore lines so long descriptions stay readable
            for (String line : MessageUtil.wrapText(guild.getDescription(), DESCRIPTION_LINE_LENGTH)) {
                text.add("<gray>" + line);
            }
        }
        text.add("");
        text.add("<yellow>괧 View members");
        if (!viewerInGuild) {
            text.add("<green>곘 Request to join");
        }
        return new StaticGuiElement('d', head, click -> {
            // right-click requests to join (only offered when the viewer isn't already in a guild)
            if (!viewerInGuild && click.getType().isRightClick()) {
                gui.close();
                GuildsHandler.INSTANCE.join(viewer, guild.getName());
                return true;
            }
            openPreview(entry);
            return true;
        }, text.toArray(String[]::new));
    }

    /** Resolves the guild's members from the cache (off the main thread), then opens its preview on the main thread. */
    private void openPreview(GuildOwnerEntry entry) {
        GuildCache.INSTANCE.get(entry.getGuild().getId())
                .thenAccept(cached -> Bukkit.getScheduler().runTask(
                        GardensPlayers.plugin,
                        () -> open(() -> new GuildPreviewMenu(viewer, cached.members()))));
    }
}
