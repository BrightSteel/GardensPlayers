package com.gardensmc.gardensplayers.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PermissionUtil {

    /**
     * Finds max perm, ie) gplayers.max_warps and player has gplayers.max_warps.5 would find 5
     */
    public static int getMaxPermission(Player player, String permission) {
        return player.getEffectivePermissions()
                .stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter(s -> s.startsWith(permission))
                .sorted() // or maybe reversed?
                .findFirst()
                .map(s -> Integer.valueOf(s.substring(permission.length() + 1)))
                .orElse(1);
    }
}
