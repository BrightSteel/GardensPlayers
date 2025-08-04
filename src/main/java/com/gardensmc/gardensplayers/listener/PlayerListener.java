package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // create playerEntry to trigger join message if applicable
        PlayerEntryCache.INSTANCE.get(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        new PlayerEntryHandler(e.getPlayer().getUniqueId()).updateLastLocation();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (isNewLocation(e.getFrom(), e.getTo())) {
            OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId()).resetAfkTick(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerExecuteCommand(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().startsWith("/afk")) {
            return;
        }
        OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId()).resetAfkTick(e.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        var playerData = OnlinePlayerCache.INSTANCE.getOnlinePlayerData(e.getPlayer().getUniqueId());
        playerData.resetAfkTick(e.getPlayer());
        if (playerData.getTextCapture() != null) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                playerData.getTextCapture().accept(PlainTextComponentSerializer.plainText().serialize(e.message()));
                playerData.setTextCapture(null);
            });
        }
    }

    private boolean isNewLocation(Location l1, Location l2) {
        return l1.getX() != l2.getX()
                || l1.getY() != l2.getY()
                || l1.getZ() != l1.getZ();
    }

    @EventHandler
    public void onPlayerChangeWorlds(PlayerChangedWorldEvent e) {
        var player = e.getPlayer();
        var permission = getWorldRtpPermission(player.getWorld().getEnvironment());
        if (permission != null && !player.hasPermission(permission)) {
            GardensPlayers.permission.playerAdd(player, permission);
        }
    }

    private String getWorldRtpPermission(World.Environment environment) {
        if (environment == World.Environment.THE_END) {
            return "gplayers.rtp.end";
        }
        if (environment == World.Environment.NETHER) {
            return "gplayers.rtp.nether";
        }
        return null;
    }
}
