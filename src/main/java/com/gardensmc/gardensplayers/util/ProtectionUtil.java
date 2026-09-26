package com.gardensmc.gardensplayers.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ProtectionUtil {

    private ProtectionUtil() {
    }

    // whether the player may build at this spot: WorldGuard regions and, if installed, GriefPrevention claims
    public static boolean canBuild(Player player, Location location) {
        if (!WorldGuardUtil.canBuild(player, location)) {
            return false;
        }
        return !Bukkit.getPluginManager().isPluginEnabled("GriefPrevention") || GriefPreventionUtil.canBuild(player, location);
    }
}
