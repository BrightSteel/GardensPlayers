package com.gardensmc.gardensplayers.menu.tile;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.menu.UpdateWarpMenu;
import com.gardensmc.gardensplayers.util.HeadUtil;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import de.themoep.inventorygui.GuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import java.util.UUID;

/**
 * A single warp rendered as a player-head tile inside the {@link TileMenu} warp view. Left-click warps to the
 * destination; owners can right-click to edit. The {@code isYourWarps} flag mirrors the "Your Warps" tab, which shows
 * private warps and visit counts rather than the public authorship line.
 */
public class WarpTileItem extends TileItem {

    private final WarpEntry warpEntry;
    private final boolean isYourWarps;

    public WarpTileItem(WarpEntry warpEntry, boolean isYourWarps) {
        super(false);
        this.warpEntry = warpEntry;
        this.isYourWarps = isYourWarps;
    }

    @Override
    public ItemStack getItemStack(Player viewer) {
        boolean owner = viewer.getUniqueId().equals(warpEntry.getPlayerUUID());
        ItemStack head = HeadUtil.createHead(warpEntry.getPlayerUUID());
        head.editMeta(meta -> {
            meta.displayName(MessageUtil.fromMiniMessage("<!italic>" + warpEntry.getName()));
            var lore = new ArrayList<String>();
            if (isYourWarps) {
                lore.add(warpEntry.isPublicAccess() ? "<green>Public" : "<red>Private");
            }
            if (warpEntry.getDescription() != null) {
                lore.add("<gray>" + warpEntry.getDescription());
            }
            if (!isYourWarps) {
                lore.add(warpEntry.isServerWarp()
                        ? "<blue>Server Warp"
                        : "<gray>by " + ownerName(warpEntry.getPlayerUUID()));
            }
            if (isYourWarps) {
                lore.add("<gray>" + warpEntry.getVisits() + " Unique Visits");
            }
            lore.add("");
            lore.add("<white>괧 <gray>Warp");
            if (owner) {
                lore.add("<white>곘 <gray>Edit");
            }
            meta.lore(lore.stream().map(line -> "<!italic>" + line).map(MessageUtil::fromMiniMessage).toList());
        });
        return head;
    }

    @Override
    public void onClick(GuiElement.Click click, TileMenu tileMenu) {
        Player player = (Player) click.getWhoClicked();
        if (player.getUniqueId().equals(warpEntry.getPlayerUUID()) && click.getType() == ClickType.RIGHT) {
            tileMenu.open(() -> new UpdateWarpMenu(player, warpEntry));
            return;
        }
        player.closeInventory();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Warping to " + warpEntry.getName()));
        WarpsHandler.INSTANCE.travel(player, warpEntry);
    }

    @Override
    public TileColor getTileColor(Player viewer) {
        return TileColor.GRAY;
    }

    // drawn once per warp: use the cached entry or the server's name cache, never a database query per owner
    private static String ownerName(UUID owner) {
        var entry = PlayerEntryCache.INSTANCE.getIfLoaded(owner);
        if (entry != null) {
            return entry.getUsername();
        }
        var name = Bukkit.getOfflinePlayer(owner).getName();
        return name != null ? name : "unknown";
    }
}
