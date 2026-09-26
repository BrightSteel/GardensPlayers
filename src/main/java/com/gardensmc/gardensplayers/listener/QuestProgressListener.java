package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardenscrops.event.CropHarvestEvent;
import com.gardensmc.gardensenchants.events.GardensEnchantItemEvent;
import com.gardensmc.gardensplayers.quest.PlacedBlocks;
import com.gardensmc.gardensplayers.quest.QuestHandler;
import com.gardensmc.gardensplayers.quest.TaskCategory;
import net.momirealms.customfishing.api.event.FishingResultEvent;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Translates Bukkit events into daily-quest progress. Each handler normalizes its subject to a Material/EntityType
 * name and forwards it to {@link QuestHandler#progress}; the handler matches it against the day's tasks. Events run
 * on the main thread, so reward granting from completion is main-thread safe.
 */
public class QuestProgressListener implements Listener {

    // ---- Mining + Farming + Foraging all come from breaking blocks ----
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        var block = e.getBlock();
        var blockName = block.getType().name();
        // Mining and Foraging tasks only match their own material sets, so forwarding every break is harmless.
        // Blocks a player just placed don't count, or placing and breaking stone would complete them. Breaking your
        // own block takes back its place progress, so place-and-break doesn't complete place tasks either.
        var placer = PlacedBlocks.takePlacer(block);
        if (placer == null) {
            QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.MINING, blockName, 1);
            QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.FORAGING, blockName, 1);
        } else if (placer.equals(e.getPlayer().getUniqueId())) {
            QuestHandler.INSTANCE.revokeProgress(e.getPlayer(), TaskCategory.BLOCK, blockName, 1);
        }
        // Farming only counts fully-grown crops.
        if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
            QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.FARMING, blockName, 1);
        }
    }

    // ---- Farming: custom GardensCrops (key = crop name, e.g. "tomato") ----
    @EventHandler
    public void onCropHarvest(CropHarvestEvent e) {
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.FARMING, e.getCropName(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        PlacedBlocks.markPlaced(e.getBlockPlaced(), e.getPlayer().getUniqueId());
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.BLOCK, e.getBlockPlaced().getType().name(), 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        var killer = e.getEntity().getKiller();
        if (killer != null) {
            QuestHandler.INSTANCE.progress(killer, TaskCategory.COMBAT, e.getEntity().getType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player player) {
            QuestHandler.INSTANCE.progress(player, TaskCategory.BREEDING, e.getEntity().getType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent e) {
        if (e.getOwner() instanceof Player player) {
            QuestHandler.INSTANCE.progress(player, TaskCategory.TAMING, e.getEntity().getType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent e) {
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.SHEAR, e.getEntity().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMilk(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Cow)) {
            return;
        }
        var hand = e.getPlayer().getInventory().getItem(e.getHand());
        if (hand != null && hand.getType() == Material.BUCKET) {
            QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.MILKING, "COW", 1);
        }
    }

    // ---- Fishing via CustomFishing (key = loot id, e.g. "cod"); replaces the vanilla fish event ----
    @EventHandler(ignoreCancelled = true)
    public void onCustomFishing(FishingResultEvent e) {
        if (e.getResult() != FishingResultEvent.Result.SUCCESS || e.getLoot() == null) {
            return;
        }
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.FISHING, e.getLoot().id(), Math.max(1, e.getAmount()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.CONSUME, e.getItem().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        var result = e.getInventory().getResult();
        if (result == null || result.getType().isAir()) {
            return;
        }
        int crafted = result.getAmount();
        if (e.isShiftClick()) {
            int maxCrafts = Integer.MAX_VALUE;
            for (ItemStack ingredient : e.getInventory().getMatrix()) {
                if (ingredient != null && !ingredient.getType().isAir()) {
                    maxCrafts = Math.min(maxCrafts, ingredient.getAmount());
                }
            }
            if (maxCrafts == Integer.MAX_VALUE) {
                maxCrafts = 1;
            }
            crafted = result.getAmount() * maxCrafts;
        }
        QuestHandler.INSTANCE.progress(player, TaskCategory.CRAFTING, result.getType().name(), crafted);
    }

    // ---- Cooking + Smelting both come from extracting furnace output ----
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent e) {
        var type = e.getItemType();
        var category = type.isEdible() ? TaskCategory.COOKING : TaskCategory.SMELTING;
        QuestHandler.INSTANCE.progress(e.getPlayer(), category, type.name(), e.getItemAmount());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player player) {
            QuestHandler.INSTANCE.progress(player, TaskCategory.THROWING, e.getEntity().getType().name(), 1);
        }
    }

    @EventHandler
    public void onEnchant(GardensEnchantItemEvent e) {
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.ENCHANTING, e.getItem().getType().name(), 1);
    }

    // ---- Location: visit a biome (key = full biome key, e.g. "minecraft:desert" or "gardens:maple_forest") ----
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        var to = e.getTo();
        var from = e.getFrom();
        // only check when the player actually changes block position, not on every look movement
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        var biomeKey = to.getBlock().getBiome().getKey().toString();
        QuestHandler.INSTANCE.progress(e.getPlayer(), TaskCategory.LOCATION, biomeKey, 1);
    }
}
