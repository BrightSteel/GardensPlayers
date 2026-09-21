package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import com.gardensmc.gardensplayers.friends.FriendsHandler;
import com.gardensmc.gardensplayers.menu.TileMenu;
import de.themoep.inventorygui.GuiElement;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A request someone has sent to the viewer. Left-click accepts it, right-click denies it.
 */
public class IncomingFriendRequestTileItem extends FriendRequestTileItem {

    public IncomingFriendRequestTileItem(FriendEntry friendEntry) {
        super(friendEntry);
    }

    @Override
    protected List<String> actionLore() {
        return List.of("", "<green>Left-click to accept", "<red>Right-click to deny");
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        String requesterName = friendEntry.otherUsername(player.getUniqueId());
        Runnable refresh = () -> FriendsHandler.INSTANCE.refreshMenu(player, tileMenu);
        if (click.getType().isRightClick()) {
            FriendsHandler.INSTANCE.deny(player, requesterName, refresh);
        } else {
            FriendsHandler.INSTANCE.add(player, requesterName, refresh);
        }
    }
}
