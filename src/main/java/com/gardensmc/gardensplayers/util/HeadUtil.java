package com.gardensmc.gardensplayers.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class HeadUtil {

    public static ItemStack createHead(Player player) {
        return createHead(player.getPlayerProfile(), true);
    }

    public static ItemStack createHead(UUID playerUUID) {
        return createHead(Bukkit.createProfile(playerUUID), true);
    }

    private static ItemStack createHead(PlayerProfile profile, boolean flat) {
        if (flat) {
            var head = GardensFurniture.itemRegistry.buildItem("2d_head");
            head.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile));
            return head;
        }
        return ItemType.PLAYER_HEAD.createItemStack(meta -> meta.setPlayerProfile(profile));
    }
}
