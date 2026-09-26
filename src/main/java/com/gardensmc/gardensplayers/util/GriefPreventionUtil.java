package com.gardensmc.gardensplayers.util;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

// Kept apart from ProtectionUtil so GriefPrevention's classes are only loaded when the plugin is installed.
final class GriefPreventionUtil {

    private GriefPreventionUtil() {
    }

    static boolean canBuild(Player player, Location location) {
        return GriefPrevention.instance.allowBuild(player, location) == null;
    }
}
