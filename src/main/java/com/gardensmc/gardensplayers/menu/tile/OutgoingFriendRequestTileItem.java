package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import com.gardensmc.gardensplayers.friends.FriendsHandler;
import com.gardensmc.gardensplayers.menu.TileMenu;
import de.themoep.inventorygui.GuiElement;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A request the viewer has sent that is still awaiting the other player's response. Clicking cancels it.
 */
public class OutgoingFriendRequestTileItem extends FriendRequestTileItem {

    public OutgoingFriendRequestTileItem(FriendEntry friendEntry) {
        super(friendEntry);
    }

    @Override
    protected List<String> actionLore() {
        return List.of("", "<gray>Request pending", "<red>Click to cancel");
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        String addresseeName = friendEntry.otherUsername(player.getUniqueId());
        FriendsHandler.INSTANCE.cancel(player, addresseeName, () -> FriendsHandler.INSTANCE.refreshMenu(player, tileMenu));
    }
}
