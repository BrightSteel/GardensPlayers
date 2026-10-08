package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.util.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        var world = e.getWorld();
        // universal game rules
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.LOCATOR_BAR, false);
        world.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 200);
        if (world.getName().contains("resource")) {
            // centered on 0,0, so RTP (which keeps inside the border) and pregeneration agree
            world.getWorldBorder().setCenter(0, 0);
            world.getWorldBorder().setSize(GardensPlayers.config.resourceRadius(world.getName()) * 2);
        }

        // sync time
        if (!world.getName().equals(Worlds.GARDENS)) {
            var mainWorld = Bukkit.getWorld(Worlds.GARDENS);
            if (mainWorld != null) {
                world.setTime(mainWorld.getTime());
            }
        }
    }
}
