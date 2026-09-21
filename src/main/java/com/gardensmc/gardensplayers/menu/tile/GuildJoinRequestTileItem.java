package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.database.entry.GuildJoinRequestEntry;
import com.gardensmc.gardensplayers.guild.GuildsHandler;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.themoep.inventorygui.GuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

/**
 * A pending request from a player to join the guild. Left-click accepts it, right-click denies it; either way the tile
 * menu is refreshed afterward.
 */
public class GuildJoinRequestTileItem extends TileItem {

    private final GuildJoinRequestEntry request;

    public GuildJoinRequestTileItem(GuildJoinRequestEntry request) {
        super(false);
        this.request = request;
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        Player online = Bukkit.getPlayer(request.getRequesterUUID());
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(request.getRequesterUUID());
        head.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><white>" + request.getRequesterUsername()));
            var lore = new ArrayList<String>();
            lore.add(online != null ? "<green>Online" : "<gray>Offline");
            lore.add("");
            lore.add("<green>Left-click to accept");
            lore.add("<red>Right-click to deny");
            meta.lore(lore.stream().map(line -> "<!italic>" + line).map(MessageUtil::fromMiniMessage).toList());
        });
        return head;
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        String requesterName = request.getRequesterUsername();
        Runnable refresh = () -> GuildsHandler.INSTANCE.refreshGuildMenu(player, tileMenu);
        if (click.getType().isRightClick()) {
            GuildsHandler.INSTANCE.deny(player, requesterName, refresh);
        } else {
            GuildsHandler.INSTANCE.accept(player, requesterName, refresh);
        }
    }

    @Override
    public TileColor getTileColor(Player viewer) {
        return TileColor.GRAY;
    }
}
