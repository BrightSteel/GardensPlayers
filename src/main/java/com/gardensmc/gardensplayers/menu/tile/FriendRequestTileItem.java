package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared rendering for a pending friend-request tile (the head of the other party plus their online status).
 * Subclasses supply the extra lore and click behaviour for incoming vs outgoing requests.
 */
public abstract class FriendRequestTileItem extends TileItem {

    protected final FriendEntry friendEntry;

    protected FriendRequestTileItem(FriendEntry friendEntry) {
        super(false);
        this.friendEntry = friendEntry;
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        UUID otherUUID = friendEntry.otherUUID(viewer.getUniqueId());
        String name = friendEntry.otherUsername(viewer.getUniqueId());
        Player online = Bukkit.getPlayer(otherUUID);
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(otherUUID);
        head.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><white>" + name));
            var lore = new ArrayList<String>();
            lore.add(online != null ? "<green>Online" : "<gray>Offline");
            lore.addAll(actionLore());
            meta.lore(lore.stream().map(line -> "<!italic>" + line).map(MessageUtil::fromMiniMessage).toList());
        });
        return head;
    }

    /** Extra lore lines shown below the online/offline status (e.g. click hints). */
    protected abstract List<String> actionLore();

    @Override
    public TileColor getTileColor(Player viewer) {
        return TileColor.GRAY;
    }
}
