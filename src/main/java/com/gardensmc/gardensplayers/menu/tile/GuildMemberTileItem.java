package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import com.gardensmc.gardensplayers.guild.GuildRole;
import com.gardensmc.gardensplayers.menu.GuildManageMemberMenu;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.themoep.inventorygui.GuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class GuildMemberTileItem extends TileItem {

    private final GuildMemberEntry guildMemberEntry;
    /** The viewing player's own role in this guild, or {@code null} if they aren't a member. Gates the manage action. */
    @Nullable
    private final GuildRole viewerRole;

    public GuildMemberTileItem(GuildMemberEntry guildMemberEntry, @Nullable GuildRole viewerRole) {
        super(false);
        this.guildMemberEntry = guildMemberEntry;
        this.viewerRole = viewerRole;
    }

    /** Only officers and above may manage members. */
    private boolean canManage() {
        return viewerRole != null && viewerRole.isAtLeast(GuildRole.OFFICER);
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        Player online = Bukkit.getPlayer(guildMemberEntry.getMemberUUID());
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(guildMemberEntry.getMemberUUID());
        head.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><white>" + guildMemberEntry.getMemberUsername()));
            var lore = new ArrayList<String>();
            lore.add("<gray>" + guildMemberEntry.getRole().getReadableName());
            lore.add(online != null ? "<green>Online" : "<gray>Offline");
            if (canManage()) {
                lore.add("");
                lore.add("<yellow>Click to manage");
            }
            meta.lore(lore.stream().map(line -> "<!italic>" + line).map(MessageUtil::fromMiniMessage).toList());
        });
        return head;
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        if (!canManage()) {
            return;
        }
        new GuildManageMemberMenu(player, guildMemberEntry, viewerRole).open();
    }

    @Override
    public TileColor getTileColor(Player viewer) {
        return Bukkit.getPlayer(guildMemberEntry.getMemberUUID()) != null ? TileColor.GREEN : TileColor.GRAY;
    }
}
