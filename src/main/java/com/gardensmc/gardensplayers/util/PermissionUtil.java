package com.gardensmc.gardensplayers.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PermissionUtil {

    /**
     * Finds max perm, ie) gplayers.max_warps and player has gplayers.max_warps.5 would find 5. Compares the
     * numbers (a text max put .9 above .10), ignores nodes set to false and anything that isn't a plain number
     * (a wildcard like gplayers.max_warps.* used to throw), and defaults to 1.
     */
    public static int getMaxPermission(Player player, String permission) {
        var prefix = permission + ".";
        return player.getEffectivePermissions()
                .stream()
                .filter(PermissionAttachmentInfo::getValue)
                .map(PermissionAttachmentInfo::getPermission)
                .filter(node -> node.startsWith(prefix))
                .map(node -> node.substring(prefix.length()))
                .filter(suffix -> suffix.matches("\\d{1,9}"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1);
    }
}
