package com.gardensmc.gardensplayers.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldGuardUtil {

    public static ApplicableRegionSet getRegionsAt(Location location) {
        RegionManager rgManager = getWorldRegions(location.getWorld());
        if (rgManager == null) return null;
        return rgManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getY(), location.getZ()));
    }

    // whether WorldGuard lets this player build here (region flags and membership, or a bypass)
    public static boolean canBuild(Player player, Location location) {
        var localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        var platform = WorldGuard.getInstance().getPlatform();
        if (platform.getSessionManager().hasBypass(localPlayer, localPlayer.getWorld())) {
            return true;
        }
        return platform.getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(location), localPlayer);
    }

    private static RegionManager getWorldRegions(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        var worldGuardWorld = BukkitAdapter.adapt(world);
        return container.get(worldGuardWorld);
    }
}
