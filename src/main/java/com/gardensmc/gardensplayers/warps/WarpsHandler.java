package com.gardensmc.gardensplayers.warps;

import com.gardensmc.gardensplayers.ActionResult;
import com.gardensmc.gardensplayers.cache.CachedWarps;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import com.gardensmc.gardensplayers.menu.tile.TileCategory;
import com.gardensmc.gardensplayers.menu.tile.WarpTileItem;
import com.gardensmc.gardensplayers.util.PermissionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WarpsHandler {

    public static final WarpsHandler INSTANCE = new WarpsHandler();

    /**
     * Background glyph for the warp tile menu title. Each tile menu has its own header texture in the resource pack
     * (ĸ shop, Ŝ friends, ŝ guild, ...); Ş must map to a "Warps" header there.
     */
    private static final char TILE_BACKGROUND = '\u0162';

    private CachedWarps cachedWarps;

    public CachedWarps getCachedWarps() {
        if (cachedWarps == null) {
            try {
                cachedWarps = CachedWarps.create(CompletableFuture.supplyAsync(Tables.warpTable::getWarpEntries).get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cachedWarps;
    }

    public void clearCachedWarps() {
        cachedWarps = null;
    }

    /** Opens the tile-based warp menu, one tab per {@link WarpEntry.Category} plus a trailing "Your Warps" tab. */
    public void openMenu(Player player) {
        TileMenu.builder(player, buildCategoryPages(player), TILE_BACKGROUND).open();
    }

    private CategoryPages buildCategoryPages(Player viewer) {
        var cached = getCachedWarps();
        var categories = new ArrayList<TileCategory>();
        for (WarpEntry.Category category : WarpEntry.Category.values()) {
            var warps = cached.warpsByCategory().getOrDefault(category, Collections.emptyList());
            categories.add(new TileCategory(
                    category.toString(),
                    category.getName(),
                    "warp_menu_icon_" + category.toString().toLowerCase(),
                    warps.stream().map(warp -> new WarpTileItem(warp, false)).toList()
            ));
        }
        var yourWarps = cached.warpsByPlayer().getOrDefault(viewer.getUniqueId(), Collections.emptyList());
        categories.add(new TileCategory(
                "your_warps",
                "Your Warps",
                "icon_profile",
                yourWarps.stream().map(warp -> new WarpTileItem(warp, true)).toList()
        ));
        return new CategoryPages(categories);
    }

    public ActionResult createWarp(Player player, String name, Location location, WarpEntry.Category category, boolean isPublic, String description) {
        var maxWarps = PermissionUtil.getMaxPermission(player, "gplayers.max_warps");
        var playerWarps = getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
        if (playerWarps.size() >= maxWarps) {
            return ActionResult.failure("You can't create more than " + maxWarps + " warp(s)!");
        }
        try {
            CompletableFuture.runAsync(() -> Tables.warpTable.createWarpEntry(
                    player,
                    name,
                    LocationJsonEntry.fromLocation(location),
                    category,
                    isPublic,
                    description
            )).get();
            clearCachedWarps();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ActionResult.success();
    }

    public CompletableFuture<Void> updateWarpMetadata(int warpId, String warpName, WarpEntry.Category category, boolean isPublic, String description) {
        return CompletableFuture
                    .runAsync(() -> Tables.warpTable.updateWarpMetadata(warpId, warpName, category, isPublic, description))
                    .thenRun(this::clearCachedWarps);
    }

    public CompletableFuture<Void> updateWarpMetadata(int warpId, String name) {
        return CompletableFuture
                .runAsync(() -> Tables.warpTable.updateWarpName(warpId, name))
                .thenRun(this::clearCachedWarps);
    }

    public CompletableFuture<Void> updateWarpLocation(int warpId, Location location) {
        return CompletableFuture
                .runAsync(() -> Tables.warpTable.updateWarpLocation(warpId, LocationJsonEntry.fromLocation(location)))
                .thenRun(this::clearCachedWarps);
    }

    public ActionResult removeWarp(Player player, String name) {
        var warp = getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList())
                .stream()
                .filter(warpEntry -> warpEntry.getReadableName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (warp == null) {
            return ActionResult.failure("Could not find warp by name: " + name);
        }
        try {
            removeWarp(warp.getId()).get();
            return ActionResult.success();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> removeWarp(int id) {
        return CompletableFuture
                .runAsync(() -> Tables.warpTable.removeWarpEntry(id))
                .thenRun(this::clearCachedWarps);
    }

    public boolean canPlayerAccess(Player player, WarpEntry warpEntry) {
        return warpEntry.isPublicAccess()
                || warpEntry.isServerWarp()
                || warpEntry.getPlayerUUID() == player.getUniqueId()
                || player.hasPermission("gplayers.warp.any");
    }

    public void travel(Player player, WarpEntry warpEntry) {
        CompletableFuture.runAsync(() -> Tables.warpVisitsTable.createWarpVisitIfNotExists(warpEntry.getId(), player.getUniqueId()));
        var location = warpEntry.getLocation().toLocation();
        if (location.getWorld() == null) { // uuid is no longer referencing a valid world
            player.sendMessage(ChatColor.RED + "Cannot travel to warp - resource world was reset");
        } else {
            player.teleport(location);
        }
    }
}
