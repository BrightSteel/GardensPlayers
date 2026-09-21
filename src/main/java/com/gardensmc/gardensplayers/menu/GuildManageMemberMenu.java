package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import com.gardensmc.gardensplayers.guild.GuildRole;
import com.gardensmc.gardensplayers.util.HeadUtil;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Management actions for a single guild member, opened from a {@link com.gardensmc.gardensplayers.menu.tile.GuildMemberTileItem}.
 * Receives the viewer's own {@link GuildRole} so it can decide which actions (kick, promote, demote, …) the viewer is
 * allowed to perform against this member.
 */
public class GuildManageMemberMenu extends Menu {

    private static final String[] LAYOUT = new String[]{
            "         ",
            "    a    ",
            "         "
    };

    private final GuildMemberEntry target;
    /** The viewing player's role in the guild — gates which management actions are offered. */
    private final GuildRole viewerRole;

    public GuildManageMemberMenu(Player viewer, GuildMemberEntry target, GuildRole viewerRole) {
        super(LAYOUT, "Manage " + target.getMemberUsername(), viewer);
        this.target = target;
        this.viewerRole = viewerRole;
        initElements();
    }

    private void initElements() {
        var online = Bukkit.getPlayer(target.getMemberUUID());
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(target.getMemberUUID());
        gui.addElement(new StaticGuiElement('a', head, click -> true,
                "<yellow>" + target.getMemberUsername(),
                "<gray>" + target.getRole().getReadableName()));
    }
}
