package com.gardensmc.gardensplayers.warps;

import com.gardensmc.gardensplayers.ActionResult;
import com.gardensmc.gardensplayers.cache.CachedWarps;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import com.gardensmc.gardensplayers.util.PermissionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WarpsHandler {

    public static final WarpsHandler INSTANCE = new WarpsHandler();

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

    public void travel(Player player, WarpEntry warpEntry) {
        CompletableFuture.runAsync(() -> Tables.warpVisitsTable.createWarpVisitIfNotExists(warpEntry.getId(), player.getUniqueId()));
        player.teleport(warpEntry.getLocation().toLocation());
    }
}
