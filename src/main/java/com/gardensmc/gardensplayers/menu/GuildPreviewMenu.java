package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import com.gardensmc.gardensplayers.database.entry.GuildOwnerEntry;
import com.gardensmc.gardensplayers.guild.GuildsHandler;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed view of a single guild, opened by clicking a guild in the {@link GuildsMenu}. The back button returns to the
 * (re-fetched) guilds list.
 */
public class GuildPreviewMenu extends Menu {

    private static final int DESCRIPTION_LINE_LENGTH = 20;

    private static final String[] LAYOUT = new String[]{
            "       a ",
            "         ",
            " bbbbbbb ",
            " bbbbbbb ",
            " bbbbbbb ",
            " cc   dd "
    };

    private final List<GuildMemberEntry> members;

    public GuildPreviewMenu(Player viewer, List<GuildMemberEntry> members) {
        super(LAYOUT, "<font:gardens:default>" + getShift(-8) + "\u015F</font>", viewer);
        this.members = members;
        initElements();
    }

    private void initElements() {

        var guildGroup = new GuiElementGroup('b');
        guildGroup.addElements(members.stream().map(this::buildMemberElement).toList());
        gui.addElement(guildGroup);

        gui.addElement(new GuiPageElement('c', blankItem, GuiPageElement.PageAction.PREVIOUS, "Previous"));
        gui.addElement(new GuiPageElement('d', blankItem, GuiPageElement.PageAction.NEXT, "Next"));

        gui.addElement(new StaticGuiElement('a', blankItem,
                click -> {
                    // openMenu re-fetches the guild list off-thread and shows it; drop history so back doesn't stack
                    InventoryGui.clearHistory(viewer);
                    GuildsHandler.INSTANCE.openMenu(viewer);
                    return true;
                }, "<gold>Back"));
    }

    private GuiElement buildMemberElement(GuildMemberEntry member) {
        var online = Bukkit.getPlayer(member.getMemberUUID());
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(member.getMemberUUID());
        return new StaticGuiElement('b', head, click -> true,
                "<yellow>" + member.getMemberUsername(),
                "<gray>" + member.getRole().getReadableName());
    }
}
