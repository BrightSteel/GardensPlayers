package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.util.WorldGuardUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

@Getter
@Setter
public class OnlinePlayerData {

    private static final int AUTO_AFK_IN_TICKS = 6000;
    private static final int AFK_KICK_IN_TICKS = 36000; // 30 minutes without moving

    private boolean AFK;
    private int afkTick;
    private int rtpCooldown;
    private int wingsBoostCooldown;
    @Nullable
    private List<Entity> nearbyEntities;
    private Consumer<String> textCapture;

    public void tick(Player player, int ticksPassed) {
        afkTick += ticksPassed;
        if (!AFK && afkTick >= AUTO_AFK_IN_TICKS) {
            AFK = true;
            Bukkit.getServer().sendMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.text(" is now AFK"))
                    .color(NamedTextColor.LIGHT_PURPLE)
            );
        } else if (AFK && afkTick >= AFK_KICK_IN_TICKS && !player.hasPermission("gplayers.afk.kick_exempt")) {
            player.kick(MiniMessage.miniMessage().deserialize("You were kicked for being AFK"));
            Bukkit.getServer().sendMessage(Component.text()
                    .append(player.displayName())
                    .append(Component.text(" was kicked for being AFK"))
                    .color(NamedTextColor.LIGHT_PURPLE)
            );
            return;
        }
        if (rtpCooldown > 0) {
            rtpCooldown -= ticksPassed;
        }
        if (wingsBoostCooldown > 0) {
            wingsBoostCooldown -= ticksPassed;
        }
        var regionSet = WorldGuardUtil.getRegionsAt(player.getLocation());
        if (regionSet != null && regionSet.getRegions().stream().anyMatch(rg -> rg.getId().contains("shop"))) {
            nearbyEntities = player.getNearbyEntities(5, 5, 5);
        } else {
            nearbyEntities = null;
        }
    }

    // leaving the server: stop being AFK without announcing they're back
    public void clearAfk() {
        AFK = false;
        afkTick = 0;
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
