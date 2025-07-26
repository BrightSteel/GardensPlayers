package com.gardensmc.gardensplayers.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;

public class WorldGuardUtil {

    public static ApplicableRegionSet getRegionsAt(Location location) {
        RegionManager rgManager = getWorldRegions(location.getWorld());
        if (rgManager == null) return null;
        return rgManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getY(), location.getZ()));
    }

    private static RegionManager getWorldRegions(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        var worldGuardWorld = BukkitAdapter.adapt(world);
        return container.get(worldGuardWorld);
    }
}
