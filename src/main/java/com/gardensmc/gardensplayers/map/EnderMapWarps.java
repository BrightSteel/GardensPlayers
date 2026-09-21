package com.gardensmc.gardensplayers.map;

import com.enderbeam.endermap.api.MapPin;
import com.enderbeam.endermap.api.MapPinProvider;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Warps on EnderMap's web map, as a layer of pins visitors can switch off.
 *
 * <p>Only warps anybody may use: public ones and server warps, the ones {@code /warp} lets anyone
 * travel to (WarpsHandler#canPlayerAccess). Private warps never go on the map, and neither does who
 * owns a warp: the map is public, and on a server whose map Enderbeam hosts, so is the internet.
 *
 * <p>EnderMap asks on the main thread every minute, so this never waits on the database: it answers
 * with what it last read and, once that is a minute old, reads again in the background. Its own
 * copy rather than WarpsHandler's cache, which blocks on the database when it is empty.
 *
 * <p>Only loaded when EnderMap is installed: these classes are EnderMap's.
 */
public final class EnderMapWarps implements MapPinProvider {

    private static final long MAX_AGE_MS = 60_000;
    /** Server warps in a colour of their own; public warps take the map's usual pin colour. */
    private static final String SERVER_COLOR = "#b48cff";

    private volatile List<WarpEntry> warps = List.of();
    private volatile long loadedAt;
    private final AtomicBoolean loading = new AtomicBoolean();

    public static void register(Plugin plugin) {
        plugin.getServer().getServicesManager().register(MapPinProvider.class, new EnderMapWarps(), plugin, ServicePriority.Normal);
    }

    @Override
    public String id() {
        return "gardensplayers-warps";
    }

    @Override
    public String label() {
        return "Warps";
    }

    @Override
    public Collection<MapPin> pins(World world) {
        refreshIfOld();
        List<MapPin> out = new ArrayList<>();
        for (WarpEntry w : warps) {
            if (!w.isPublicAccess() && !w.isServerWarp()) continue;
            var at = w.getLocation();
            if (at == null || !world.getUID().equals(at.getWorld())) continue;
            String category = w.getCategory() == null ? null : w.getCategory().getName();
            String detail = w.isServerWarp() ? (category == null ? "Server warp" : "Server warp · " + category) : category;
            out.add(new MapPin(w.getReadableName(), at.getX(), at.getY(), at.getZ(), detail, w.isServerWarp() ? SERVER_COLOR : null));
        }
        return out;
    }

    private void refreshIfOld() {
        if (System.currentTimeMillis() - loadedAt <= MAX_AGE_MS || !loading.compareAndSet(false, true)) return;
        CompletableFuture.supplyAsync(Tables.warpTable::getWarpEntries).whenComplete((all, error) -> {
            try {
                if (all != null) {
                    warps = List.copyOf(all);
                    loadedAt = System.currentTimeMillis();
                }
            } finally {
                loading.set(false);
            }
        });
    }
}
