package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.themoep.inventorygui.GuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

public class FriendTileItem extends TileItem {

    private final FriendEntry friendEntry;

    public FriendTileItem(FriendEntry friendEntry) {
        super(false);
        this.friendEntry = friendEntry;
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        UUID friendUUID = friendEntry.otherUUID(viewer.getUniqueId());
        String name = friendEntry.otherUsername(viewer.getUniqueId());
        Player online = Bukkit.getPlayer(friendUUID);
        ItemStack head = online != null ? HeadUtil.createHead(online) : HeadUtil.createHead(friendUUID);
        head.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><white>" + name));
            var lore = new ArrayList<String>();
            lore.add(online != null ? "<green>Online" : "<gray>Offline");
            if (online != null) {
                lore.add("");
                lore.add("<yellow>Click to teleport");
            }
            meta.lore(lore.stream().map(line -> "<!italic>" + line).map(MessageUtil::fromMiniMessage).toList());
        });
        return head;
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        UUID friendUUID = friendEntry.otherUUID(player.getUniqueId());
        String name = friendEntry.otherUsername(player.getUniqueId());
        Player friend = Bukkit.getPlayer(friendUUID);
        if (friend == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + name + " is not online"));
            return;
        }
        player.closeInventory();
        player.teleport(friend);
    }

    @Override
    public TileColor getTileColor(Player viewer) {
        if (Bukkit.getPlayer(friendEntry.otherUUID(viewer.getUniqueId())) != null) {
            return TileColor.GREEN;
        }
        return TileColor.GRAY;
    }
}
