package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.util.ProbabilityUtil;
import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class BlockListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent e) {
        if (ProbabilityUtil.roll(2)) {
            dropAcorn(e.getBlock().getLocation(), true);
        }
        if (e.getBlock().getType().toString().toLowerCase().contains("oak_leaves") && ProbabilityUtil.roll(2)) {
            dropAcorn(e.getBlock().getLocation(), false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeafBreak(BlockBreakEvent e) {
        if (e.getBlock().getType().toString().toLowerCase().endsWith("leaves") && ProbabilityUtil.roll(2)) {
            dropAcorn(e.getBlock().getLocation(), true);
        }
        if (e.getBlock().getType().toString().toLowerCase().contains("oak_leaves") && ProbabilityUtil.roll(2)) {
            dropAcorn(e.getBlock().getLocation(), false);
        }
    }

    private void dropAcorn(Location location, boolean golden) {
        var acorn = Objects.requireNonNull(GardensFurniture.itemRegistry.buildItem(golden ? "golden_acorn" : "acorn"));
        acorn.setAmount(ThreadLocalRandom.current().nextInt(1, 4));
        Bukkit.getScheduler().runTaskLater(
                GardensPlayers.plugin,
                () -> location.getWorld().dropItemNaturally(location, acorn),
                1
        );
    }
}
