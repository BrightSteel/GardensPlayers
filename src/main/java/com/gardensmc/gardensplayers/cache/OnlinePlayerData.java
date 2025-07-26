package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.util.WorldGuardUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OnlinePlayerData {

    private static final int AUTO_AFK_IN_TICKS = 6000;

    private boolean AFK;
    private int afkTick;
    @Getter
    @Setter
    private int rtpCooldown;
    @Nullable
    private List<Entity> nearbyEntities;

    public void tick(Player player, int ticksPassed) {
        afkTick += ticksPassed;
        if (!AFK && afkTick >= AUTO_AFK_IN_TICKS) {
            AFK = true;
            Bukkit.getServer().sendMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.text(" is now AFK"))
                    .color(NamedTextColor.LIGHT_PURPLE)
            );
        }
        if (rtpCooldown > 0) {
            rtpCooldown -= ticksPassed;
        }
        var regionSet = WorldGuardUtil.getRegionsAt(player.getLocation());
        if (regionSet != null && regionSet.getRegions().stream().anyMatch(rg -> rg.getId().contains("shop"))) {
            nearbyEntities = player.getNearbyEntities(5, 5, 5);
        } else {
            nearbyEntities = null;
        }
    }

    public void resetAfkTick(Player player) {
        if (AFK) {
            AFK = false;
            Bukkit.getServer().sendMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.text(" is no longer AFK"))
                    .color(NamedTextColor.LIGHT_PURPLE)
            );
        }
        afkTick = 0;
    }

}
