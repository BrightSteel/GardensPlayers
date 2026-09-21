package com.gardensmc.gardensplayers.quest;

import lombok.Getter;
import org.bukkit.Material;

/**
 * The categories of daily quest task. Each maps to one or more Bukkit events handled in
 * {@code QuestProgressListener}. Dungeon and Delivery remain omitted for now (they need a server-specific
 * dungeon/NPC system); Location and Foraging are handled with movement and block-break detection respectively.
 */
@Getter
public enum TaskCategory {

    BLOCK("Block", Material.BRICKS),
    BREEDING("Breeding", Material.WHEAT),
    COMBAT("Combat", Material.IRON_SWORD),
    CONSUME("Consume", Material.COOKED_BEEF),
    COOKING("Cooking", Material.SMOKER),
    CRAFTING("Crafting", Material.CRAFTING_TABLE),
    FARMING("Farming", Material.WHEAT),
    FISHING("Fishing", Material.FISHING_ROD),
    MILKING("Milking", Material.MILK_BUCKET),
    MINING("Mining", Material.IRON_PICKAXE),
    SHEAR("Shear", Material.SHEARS),
    TAMING("Taming", Material.BONE),
    SMELTING("Smelting", Material.FURNACE),
    THROWING("Throwing", Material.SNOWBALL),
    ENCHANTING("Enchanting", Material.ENCHANTING_TABLE),
    LOCATION("Location", Material.COMPASS),
    FORAGING("Foraging", Material.OAK_SAPLING);

    private final String displayName;
    /**
     * Fallback icon used in the quests menu when a task's target has no obvious item representation.
     */
    private final Material icon;

    TaskCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    /**
     * The GardensFurniture item id used for this category's icon in the quests menu, e.g. {@code "icon_mining"}.
     * When {@code completed} is true, {@code "_completed"} is appended (e.g. {@code "icon_mining_completed"}).
     */
    public String getIconItemId(boolean completed) {
        return "icon_" + name().toLowerCase() + "_task" + (completed ? "_completed" : "");
    }
}
