package com.gardensmc.gardensplayers.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionUtilTest {

    // a Player whose only behaviour is getEffectivePermissions (node -> granted)
    private static Player playerWith(Map<String, Boolean> permissions) {
        var infos = new HashSet<PermissionAttachmentInfo>();
        var player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class}, (proxy, method, args) -> {
            if (method.getName().equals("getEffectivePermissions")) {
                return infos;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        permissions.forEach((node, value) -> infos.add(new PermissionAttachmentInfo(player, node, null, value)));
        return player;
    }

    @Test
    void defaultsToOne() {
        assertEquals(1, PermissionUtil.getMaxPermission(playerWith(Map.of()), "gplayers.max_warps"));
    }

    @Test
    void comparesNumbersNotText() {
        var player = playerWith(Map.of("gplayers.max_warps.9", true, "gplayers.max_warps.10", true));
        assertEquals(10, PermissionUtil.getMaxPermission(player, "gplayers.max_warps"));
    }

    @Test
    void ignoresNegatedWildcardAndOtherNodes() {
        var player = playerWith(Map.of(
                "gplayers.max_warps.50", false,
                "gplayers.max_warps.*", true,
                "gplayers.max_warps.3", true,
                "gplayers.max_warps_extra.20", true,
                "gplayers.max_warps.99999999999", true));
        assertEquals(3, PermissionUtil.getMaxPermission(player, "gplayers.max_warps"));
    }
}
