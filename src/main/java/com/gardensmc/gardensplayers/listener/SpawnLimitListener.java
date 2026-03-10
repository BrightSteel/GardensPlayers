package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;

public class SpawnLimitListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onMobSpawnerSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        var limit = GardensPlayers.spawnLimits.getSpawnerLimit();
        if (limit == -1) {
            return;
        }
        var radius = GardensPlayers.spawnLimits.getRadius();
        if (getNearbyEntityCount(e.getLocation(), radius) > limit) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobBreed(EntityBreedEvent e) {
        var limit = GardensPlayers.spawnLimits.getBreedLimit();
        if (limit == -1) {
            return;
        }
        var radius = GardensPlayers.spawnLimits.getRadius();
        var nearbyEntityCount = getNearbyEntityCount(e.getEntity().getLocation(), radius);
        if (nearbyEntityCount > limit) {
            if (GardensPlayers.spawnLimits.isBreedDumbAtLimit()) {
                e.getEntity().setAI(false);
            } else {
                e.setCancelled(true);
                e.setExperience(0);
                if (e.getBreeder() instanceof Player player) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("Too many mobs nearby to breed more"));
                }
            }
        }
    }

    private int getNearbyEntityCount(Location location, int radius) {
        return (int) location.getNearbyLivingEntities(radius)
                .stream()
                .filter(livingEntity -> !(livingEntity instanceof Player))
                .count();
    }
}
