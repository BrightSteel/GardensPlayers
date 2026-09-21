package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        var world = e.getWorld();
        // universal game rules
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 200);
        if (world.getName().contains("resource")) {
            world.getWorldBorder().setSize(GardensPlayers.config.getResourceWorldRadius() * 2);
        }

        // sync time
        if (!world.getName().equals("gardens")) {
            var mainWorld = Bukkit.getWorld("gardens");
            if (mainWorld != null) {
                world.setTime(mainWorld.getTime());
            }
        }
    }
}
