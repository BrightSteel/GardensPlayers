package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.cache.GuildCache;
import com.gardensmc.gardensplayers.database.entry.GuildEntry;
import com.gardensmc.gardensplayers.guild.GuildRole;
import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/**
 * The {@code /guild} members tile menu. A {@link TileMenu} with its own layout (currently identical to the default) so
 * the guild view can diverge from the shared tile layout without affecting other menus.
 */
public class GuildMembersTileMenu extends TileMenu {

    private static final int DESCRIPTION_LINE_LENGTH = 30;
    private static final DateTimeFormatter CREATED_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private static final String[] LAYOUT = new String[] {
            "aiiiiiigh",
            "bbbbbbbbb",
            "ddddddddd",
            "ddddddddd",
            "ddddddddd",
            "eee   fff"
    };

    private final GuildCache.CachedGuild cachedGuild;

    public GuildMembersTileMenu(GuildCache.CachedGuild cachedGuild, Player viewer, CategoryPages pages, char bg, String extraTitleChars) {
        this(cachedGuild, LAYOUT, viewer, pages, 0, 0, 0, false, bg, extraTitleChars);
    }

    private GuildMembersTileMenu(GuildCache.CachedGuild cachedGuild, String[] layout, Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg, String extraTitleChars) {
        super(layout, viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg, extraTitleChars);
        this.cachedGuild = cachedGuild;
        initGuildElements();
    }

    /** Keeps the concrete type (and guild data) across navigation — paging, tab switches, refreshes. */
    @Override
    protected TileMenu create(String[] layout, Player viewer, CategoryPages pages, int page, int selectedCategory, int guiPageNumber, boolean showBalances, char bg, String extraTitleChars) {
        return new GuildMembersTileMenu(cachedGuild, layout, viewer, pages, page, selectedCategory, guiPageNumber, showBalances, bg, extraTitleChars);
    }

    private void initGuildElements() {
        gui.addElement(new StaticGuiElement(
            'g', blankItem, click -> {
                gui.close();
                viewer.chat("/guild home");
                return true;
        }, "<green>Guild Home", "<gray>Click to travel"
        ));
        // Non-clickable info button: shows the guild's details (name, member count, etc.).
        gui.addElement(new StaticGuiElement('i', blankItem, buildGuildInfo()));
    }

    /** Builds the lore lines describing the guild for the info button. */
    private String[] buildGuildInfo() {
        GuildEntry guild = cachedGuild.guild();
        if (guild == null) {
            return new String[] { "<red>This guild no longer exists" };
        }

        var lines = new ArrayList<String>();
        lines.add("<yellow>" + guild.getName());
        if (guild.getDescription() != null) {
            for (String line : MessageUtil.wrapText(guild.getDescription(), DESCRIPTION_LINE_LENGTH)) {
                lines.add("<gray>" + line);
            }
        }
        lines.add("");

        var members = cachedGuild.members();
        long online = members.stream().filter(member -> Bukkit.getPlayer(member.getMemberUUID()) != null).count();
        lines.add("<gray>Members: <white>" + members.size() + " <dark_gray>(" + online + " online)");

        members.stream()
                .filter(member -> member.getRole() == GuildRole.OWNER)
                .findFirst()
                .ifPresent(owner -> lines.add("<gray>Owner: <white>" + owner.getMemberUsername()));

        lines.add("<gray>Balance: <gold>" + NumberFormat.getNumberInstance(Locale.US).format(guild.getBalance()));
        lines.add("<gray>Created: <white>" + guild.getCreatedAt().format(CREATED_FORMATTER));

        return lines.toArray(String[]::new);
    }
}
