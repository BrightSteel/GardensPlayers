package com.gardensmc.gardensplayers.menu.shop;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Best-effort mapping from a Bukkit {@link Material} to a vanilla Bedrock texture path, used to give
 * {@link BedrockShopMenu} grid cells real item icons on Geyser clients.
 *
 * <p>Bedrock texture filenames don't line up 1:1 with Bukkit material names (and block "item" icons
 * are really 3D renders), so this is a curated best-effort table rather than an exhaustive one.
 * Anything not listed falls back to the pack's gray default icon. Tool/armor families are generated
 * to keep the table compact. Verify paths against your Bedrock version if an icon shows wrong.
 */
final class ShopItemIcons {

    private ShopItemIcons() {
    }

    private static final Map<Material, String> PATHS = new HashMap<>();

    private static void item(Material m, String name) {
        if (m != null) PATHS.put(m, "textures/items/" + name);
    }

    private static void block(Material m, String name) {
        if (m != null) PATHS.put(m, "textures/blocks/" + name);
    }

    private static Material mat(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Tool/armor families share a Bedrock naming scheme; generate them so the table stays small. */
    private static void toolFamily(String javaTier, String bedrockTier) {
        for (String tool : new String[]{"SWORD", "PICKAXE", "AXE", "SHOVEL", "HOE"}) {
            item(mat(javaTier + "_" + tool), bedrockTier + "_" + tool.toLowerCase());
        }
    }

    private static void armorFamily(String javaTier, String bedrockTier) {
        item(mat(javaTier + "_HELMET"), bedrockTier + "_helmet");
        item(mat(javaTier + "_CHESTPLATE"), bedrockTier + "_chestplate");
        item(mat(javaTier + "_LEGGINGS"), bedrockTier + "_leggings");
        item(mat(javaTier + "_BOOTS"), bedrockTier + "_boots");
    }

    static {
        // --- ingots / gems / minerals ---
        item(Material.DIAMOND, "diamond");
        item(Material.EMERALD, "emerald");
        item(Material.GOLD_INGOT, "gold_ingot");
        item(Material.IRON_INGOT, "iron_ingot");
        item(Material.NETHERITE_INGOT, "netherite_ingot");
        item(mat("NETHERITE_SCRAP"), "netherite_scrap");
        item(mat("COPPER_INGOT"), "copper_ingot");
        item(Material.COAL, "coal");
        item(Material.CHARCOAL, "charcoal");
        item(Material.GOLD_NUGGET, "gold_nugget");
        item(Material.IRON_NUGGET, "iron_nugget");
        item(Material.QUARTZ, "quartz");
        item(mat("AMETHYST_SHARD"), "amethyst_shard");
        item(Material.LAPIS_LAZULI, "dye_powder_blue");
        item(Material.REDSTONE, "redstone_dust");
        item(Material.GLOWSTONE_DUST, "glowstone_dust");
        item(Material.NETHER_STAR, "nether_star");

        // --- tools & armor (generated) ---
        toolFamily("WOODEN", "wood");
        toolFamily("STONE", "stone");
        toolFamily("IRON", "iron");
        toolFamily("GOLDEN", "gold");
        toolFamily("DIAMOND", "diamond");
        toolFamily("NETHERITE", "netherite");
        armorFamily("LEATHER", "leather");
        armorFamily("CHAINMAIL", "chain");
        armorFamily("IRON", "iron");
        armorFamily("GOLDEN", "gold");
        armorFamily("DIAMOND", "diamond");
        armorFamily("NETHERITE", "netherite");
        item(Material.BOW, "bow_standby");
        item(Material.ARROW, "arrow");
        item(mat("SHIELD"), "shield");
        item(Material.FISHING_ROD, "fishing_rod");
        item(Material.FLINT_AND_STEEL, "flint_and_steel");
        item(Material.SHEARS, "shears");

        // --- mob drops ---
        item(Material.STRING, "string");
        item(Material.BONE, "bone");
        item(Material.GUNPOWDER, "gunpowder");
        item(Material.SPIDER_EYE, "spider_eye");
        item(Material.ENDER_PEARL, "ender_pearl");
        item(Material.ENDER_EYE, "ender_eye");
        item(Material.BLAZE_ROD, "blaze_rod");
        item(Material.BLAZE_POWDER, "blaze_powder");
        item(Material.SLIME_BALL, "slimeball");
        item(Material.MAGMA_CREAM, "magma_cream");
        item(Material.GHAST_TEAR, "ghast_tear");
        item(Material.ROTTEN_FLESH, "rotten_flesh");
        item(Material.LEATHER, "leather");
        item(Material.FEATHER, "feather");
        item(Material.RABBIT_FOOT, "rabbit_foot");
        item(Material.RABBIT_HIDE, "rabbit_hide");
        item(mat("PHANTOM_MEMBRANE"), "phantom_membrane");
        item(mat("HONEYCOMB"), "honeycomb");
        item(Material.PRISMARINE_SHARD, "prismarine_shard");
        item(Material.PRISMARINE_CRYSTALS, "prismarine_crystals");
        item(Material.INK_SAC, "dye_powder_black");
        item(Material.EGG, "egg");

        // --- food / farming ---
        item(Material.WHEAT, "wheat");
        item(Material.WHEAT_SEEDS, "seeds_wheat");
        item(Material.BREAD, "bread");
        item(Material.APPLE, "apple");
        item(Material.GOLDEN_APPLE, "apple_golden");
        item(Material.CARROT, "carrot");
        item(Material.GOLDEN_CARROT, "golden_carrot");
        item(Material.POTATO, "potato");
        item(Material.BAKED_POTATO, "potato_baked");
        item(Material.BEEF, "beef_raw");
        item(Material.COOKED_BEEF, "beef_cooked");
        item(Material.PORKCHOP, "porkchop_raw");
        item(Material.COOKED_PORKCHOP, "porkchop_cooked");
        item(Material.CHICKEN, "chicken_raw");
        item(Material.COOKED_CHICKEN, "chicken_cooked");
        item(Material.SUGAR_CANE, "reeds");
        item(Material.SUGAR, "sugar");
        item(Material.NETHER_WART, "nether_wart");
        item(Material.COOKIE, "cookie");

        // --- misc items ---
        item(Material.STICK, "stick");
        item(Material.BUCKET, "bucket_empty");
        item(Material.WATER_BUCKET, "bucket_water");
        item(Material.LAVA_BUCKET, "bucket_lava");
        item(Material.MILK_BUCKET, "bucket_milk");
        item(Material.FLINT, "flint");
        item(Material.PAPER, "paper");
        item(Material.BOOK, "book_normal");
        item(Material.SNOWBALL, "snowball");
        item(Material.EXPERIENCE_BOTTLE, "experience_bottle");
        item(Material.NAME_TAG, "name_tag");
        item(Material.SADDLE, "saddle");
        item(Material.TOTEM_OF_UNDYING, "totem");

        // --- ores ---
        block(Material.COAL_ORE, "coal_ore");
        block(Material.IRON_ORE, "iron_ore");
        block(Material.GOLD_ORE, "gold_ore");
        block(Material.DIAMOND_ORE, "diamond_ore");
        block(Material.EMERALD_ORE, "emerald_ore");
        block(Material.REDSTONE_ORE, "redstone_ore");
        block(Material.LAPIS_ORE, "lapis_ore");
        block(mat("COPPER_ORE"), "copper_ore");
        block(Material.NETHER_QUARTZ_ORE, "quartz_ore");
        block(mat("NETHER_GOLD_ORE"), "nether_gold_ore");
        block(mat("DEEPSLATE_COAL_ORE"), "deepslate_coal_ore");
        block(mat("DEEPSLATE_IRON_ORE"), "deepslate_iron_ore");
        block(mat("DEEPSLATE_GOLD_ORE"), "deepslate_gold_ore");
        block(mat("DEEPSLATE_DIAMOND_ORE"), "deepslate_diamond_ore");
        block(mat("DEEPSLATE_EMERALD_ORE"), "deepslate_emerald_ore");
        block(mat("DEEPSLATE_REDSTONE_ORE"), "deepslate_redstone_ore");
        block(mat("DEEPSLATE_LAPIS_ORE"), "deepslate_lapis_ore");
        block(mat("DEEPSLATE_COPPER_ORE"), "deepslate_copper_ore");
        block(mat("ANCIENT_DEBRIS"), "ancient_debris_side");

        // --- mineral / common blocks ---
        block(Material.DIAMOND_BLOCK, "diamond_block");
        block(Material.EMERALD_BLOCK, "emerald_block");
        block(Material.GOLD_BLOCK, "gold_block");
        block(Material.IRON_BLOCK, "iron_block");
        block(mat("NETHERITE_BLOCK"), "netherite_block");
        block(Material.REDSTONE_BLOCK, "redstone_block");
        block(Material.LAPIS_BLOCK, "lapis_block");
        block(Material.COAL_BLOCK, "coal_block");
        block(mat("COPPER_BLOCK"), "copper_block");
        block(Material.STONE, "stone");
        block(Material.COBBLESTONE, "cobblestone");
        block(Material.MOSSY_COBBLESTONE, "cobblestone_mossy");
        block(Material.DIRT, "dirt");
        block(Material.SAND, "sand");
        block(Material.RED_SAND, "red_sand");
        block(Material.GRAVEL, "gravel");
        block(Material.CLAY, "clay");
        block(Material.GLASS, "glass");
        block(Material.OBSIDIAN, "obsidian");
        block(Material.NETHERRACK, "netherrack");
        block(Material.SOUL_SAND, "soul_sand");
        block(Material.GLOWSTONE, "glowstone");
        block(Material.SANDSTONE, "sandstone_normal");
        block(Material.BRICKS, "brick");
        block(Material.STONE_BRICKS, "stonebrick");
        block(Material.OBSIDIAN, "obsidian");
        block(Material.OAK_LOG, "log_oak");
        block(Material.OAK_PLANKS, "planks_oak");
        block(Material.BOOKSHELF, "bookshelf");
        block(Material.TNT, "tnt_side");
        block(Material.SPONGE, "sponge");
        block(Material.HAY_BLOCK, "hay_block_side");
        block(Material.SLIME_BLOCK, "slime");
        block(Material.SPAWNER, "mob_spawner");
        block(Material.END_STONE, "end_stone");
        block(mat("ANCIENT_DEBRIS"), "ancient_debris_side");
    }

    @Nullable
    static String texturePath(Material material) {
        return PATHS.get(material);
    }
}
