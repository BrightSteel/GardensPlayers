package com.gardensmc.gardensplayers.quest;

import org.bukkit.Material;

import java.util.List;

/**
 * Static registry of all {@link TaskTemplate}s, grouped by category. {@code QuestHandler} draws from {@link #ALL}
 * when generating the daily tasks. Targets use Material names for block/item events, EntityType names for entity
 * events, and custom ids for CustomFishing loot / GardensCrops / biomes; these must match what the corresponding
 * handler in {@code QuestProgressListener} forwards. Menu icons are per-category (see {@link TaskCategory}).
 */
public final class TaskTemplates {

    private TaskTemplates() {
    }

    public static final List<TaskTemplate> ALL = List.of(
            // ---- Mining (BlockBreakEvent on ores) ----
            new TaskTemplate(TaskCategory.MINING, "Mine {amount} {target}", List.of(
                    TaskTarget.of("Coal Ore", Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
                    TaskTarget.of("Iron Ore", Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
                    TaskTarget.of("Copper Ore", Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE),
                    TaskTarget.of("Gold Ore", Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE),
                    TaskTarget.of("Redstone Ore", Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
                    TaskTarget.of("Lapis Ore", Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
                    TaskTarget.of("Diamond Ore", Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
                    TaskTarget.of("Emerald Ore", Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE),
                    TaskTarget.of("Nether Quartz Ore", Material.NETHER_QUARTZ_ORE),
                    TaskTarget.of("Nether Gold Ore", Material.NETHER_GOLD_ORE),
                    TaskTarget.of("Ancient Debris", Material.ANCIENT_DEBRIS)
            ), 5, 24),

            // ---- Block (BlockPlaceEvent) ----
            new TaskTemplate(TaskCategory.BLOCK, "Place {amount} {target}", List.of(
                    TaskTarget.of(Material.STONE),
                    TaskTarget.of(Material.COBBLESTONE),
                    TaskTarget.of(Material.STONE_BRICKS),
                    TaskTarget.of(Material.DEEPSLATE),
                    TaskTarget.of(Material.ANDESITE),
                    TaskTarget.of(Material.GRANITE),
                    TaskTarget.of(Material.DIORITE),
                    TaskTarget.of(Material.SMOOTH_STONE),
                    TaskTarget.of(Material.OAK_PLANKS),
                    TaskTarget.of(Material.SPRUCE_PLANKS),
                    TaskTarget.of(Material.BIRCH_PLANKS),
                    TaskTarget.of(Material.OAK_LOG),
                    TaskTarget.of(Material.OAK_STAIRS),
                    TaskTarget.of(Material.OAK_SLAB),
                    TaskTarget.of(Material.OAK_FENCE),
                    TaskTarget.of(Material.GLASS),
                    TaskTarget.of(Material.GLASS_PANE),
                    TaskTarget.of(Material.BRICKS),
                    TaskTarget.of(Material.SANDSTONE),
                    TaskTarget.of(Material.SAND),
                    TaskTarget.of(Material.GRAVEL),
                    TaskTarget.of(Material.DIRT),
                    TaskTarget.of(Material.QUARTZ_BLOCK),
                    TaskTarget.of(Material.TERRACOTTA),
                    TaskTarget.of(Material.WHITE_WOOL),
                    TaskTarget.of(Material.WHITE_CONCRETE),
                    TaskTarget.of(Material.COPPER_BLOCK),
                    TaskTarget.of(Material.DEEPSLATE_BRICKS),
                    TaskTarget.of(Material.MUD_BRICKS),
                    TaskTarget.of(Material.TORCH)
            ), 8, 24),

            // ---- Farming: vanilla crops (BlockBreakEvent on mature crops; key = crop block name) ----
            new TaskTemplate(TaskCategory.FARMING, "Harvest {amount} {target}", List.of(
                    TaskTarget.keyed("Wheat", "WHEAT"),
                    TaskTarget.keyed("Carrots", "CARROTS"),
                    TaskTarget.keyed("Potatoes", "POTATOES"),
                    TaskTarget.keyed("Beetroots", "BEETROOTS"),
                    TaskTarget.keyed("Nether Wart", "NETHER_WART")
            ), 8, 32),

            // ---- Farming: custom GardensCrops (CropHarvestEvent; key = crop name) ----
            new TaskTemplate(TaskCategory.FARMING, "Harvest {amount} {target}", List.of(
                    TaskTarget.keyed("Tomatoes", "tomato"),
                    TaskTarget.keyed("Cabbages", "cabbage"),
                    TaskTarget.keyed("Corn", "corn"),
                    TaskTarget.keyed("Lemons", "lemon"),
                    TaskTarget.keyed("Lettuce", "lettuce"),
                    TaskTarget.keyed("Moolis", "moolis"),
                    TaskTarget.keyed("Pomegranates", "pomegranate"),
                    TaskTarget.keyed("Grapes", "grape"),
                    TaskTarget.keyed("Bananas", "banana"),
                    TaskTarget.keyed("Pineapples", "pineapple"),
                    TaskTarget.keyed("Oranges", "orange"),
                    TaskTarget.keyed("Strawberries", "strawberry")
            ), 6, 20),

            // ---- Combat (EntityDeathEvent, killer is player; key = EntityType name) ----
            new TaskTemplate(TaskCategory.COMBAT, "Kill {amount} {target}", List.of(
                    TaskTarget.keyed("Zombies", "ZOMBIE"),
                    TaskTarget.keyed("Skeletons", "SKELETON"),
                    TaskTarget.keyed("Spiders", "SPIDER"),
                    TaskTarget.keyed("Creepers", "CREEPER"),
                    TaskTarget.keyed("Endermen", "ENDERMAN"),
                    TaskTarget.keyed("Witches", "WITCH"),
                    TaskTarget.keyed("Slimes", "SLIME"),
                    TaskTarget.keyed("Drowned", "DROWNED"),
                    TaskTarget.keyed("Husks", "HUSK"),
                    TaskTarget.keyed("Strays", "STRAY"),
                    TaskTarget.keyed("Phantoms", "PHANTOM"),
                    TaskTarget.keyed("Cave Spiders", "CAVE_SPIDER"),
                    TaskTarget.keyed("Silverfish", "SILVERFISH"),
                    TaskTarget.keyed("Zombie Villagers", "ZOMBIE_VILLAGER"),
                    TaskTarget.keyed("Pillagers", "PILLAGER"),
                    TaskTarget.keyed("Blazes", "BLAZE"),
                    TaskTarget.keyed("Magma Cubes", "MAGMA_CUBE"),
                    TaskTarget.keyed("Piglins", "PIGLIN"),
                    TaskTarget.keyed("Hoglins", "HOGLIN"),
                    TaskTarget.keyed("Ghasts", "GHAST"),
                    TaskTarget.keyed("Wither Skeletons", "WITHER_SKELETON"),
                    TaskTarget.keyed("Guardians", "GUARDIAN")
            ), 5, 15),

            // ---- Breeding (EntityBreedEvent, breeder is player) ----
            new TaskTemplate(TaskCategory.BREEDING, "Breed {amount} {target}", List.of(
                    TaskTarget.keyed("Cows", "COW"),
                    TaskTarget.keyed("Pigs", "PIG"),
                    TaskTarget.keyed("Sheep", "SHEEP"),
                    TaskTarget.keyed("Chickens", "CHICKEN"),
                    TaskTarget.keyed("Rabbits", "RABBIT"),
                    TaskTarget.keyed("Horses", "HORSE"),
                    TaskTarget.keyed("Wolves", "WOLF"),
                    TaskTarget.keyed("Cats", "CAT"),
                    TaskTarget.keyed("Goats", "GOAT"),
                    TaskTarget.keyed("Foxes", "FOX"),
                    TaskTarget.keyed("Bees", "BEE"),
                    TaskTarget.keyed("Llamas", "LLAMA"),
                    TaskTarget.keyed("Turtles", "TURTLE"),
                    TaskTarget.keyed("Axolotls", "AXOLOTL")
            ), 2, 6),

            // ---- Taming (EntityTameEvent, owner is player) ----
            new TaskTemplate(TaskCategory.TAMING, "Tame {amount} {target}", List.of(
                    TaskTarget.keyed("Wolves", "WOLF"),
                    TaskTarget.keyed("Cats", "CAT"),
                    TaskTarget.keyed("Horses", "HORSE"),
                    TaskTarget.keyed("Parrots", "PARROT"),
                    TaskTarget.keyed("Donkeys", "DONKEY"),
                    TaskTarget.keyed("Mules", "MULE"),
                    TaskTarget.keyed("Llamas", "LLAMA")
            ), 1, 4),

            // ---- Shear (PlayerShearEntityEvent) ----
            new TaskTemplate(TaskCategory.SHEAR, "Shear {amount} {target}", List.of(
                    TaskTarget.keyed("Sheep", "SHEEP")
            ), 4, 12),

            // ---- Milking (PlayerInteractEntityEvent: cow + bucket) ----
            new TaskTemplate(TaskCategory.MILKING, "Milk {amount} {target}", List.of(
                    TaskTarget.keyed("Cows", "COW")
            ), 2, 8),

            // ---- Fishing via CustomFishing (FishingResultEvent; key = CustomFishing loot id) ----
            // Fish are grouped by their CustomFishing "group" (river / ocean / warm_ocean / loots_in_lava) so a
            // task is achievable by fishing the right water, rather than gating on one specific RNG fish.
            new TaskTemplate(TaskCategory.FISHING, "Catch {amount} {target}", List.of(
                    TaskTarget.any("Fish"),
                    TaskTarget.keyed("River Fish",
                            "amphipod", "bass", "carp", "catfish", "clam", "crab", "crayfish",
                            "freshwater_sponge", "mussel", "perch", "pike", "snail", "trout"),
                    TaskTarget.keyed("Ocean Fish",
                            "anglerfish", "barracuda", "barreleye_fish", "blobfish", "dragon_fish", "dumbo_octopus",
                            "giant_isopod", "grouper", "gulper_eel", "herring", "lanternfish", "mackerel", "marlin",
                            "redfish", "sardine", "snapper", "snook", "sword_fish", "tuna", "vampire_squid",
                            "viper_fish", "wahoo"),
                    TaskTarget.keyed("Warm Ocean Fish",
                            "angelfish", "butterflyfish", "damsel_fish", "lion_fish", "moray_eel", "parrotfish",
                            "surgeonfish", "trigger_fish"),
                    TaskTarget.keyed("Nether Fish",
                            "ashmaw", "soulfin", "blightscale", "bone_dart", "cinder_pike", "ember_carp", "glowsnap",
                            "glowtail", "hellpuffer", "hellstripe", "lava_lantern", "magma_belly", "nether_ray",
                            "rustfin", "spore_jelly", "warped_eel", "moltenjaw")
            ), 5, 15),

            // ---- Consume (PlayerItemConsumeEvent) ----
            new TaskTemplate(TaskCategory.CONSUME, "Eat {amount} {target}", List.of(
                    TaskTarget.of(Material.BREAD),
                    TaskTarget.of(Material.COOKED_BEEF),
                    TaskTarget.of(Material.COOKED_CHICKEN),
                    TaskTarget.of(Material.COOKED_PORKCHOP),
                    TaskTarget.of(Material.COOKED_MUTTON),
                    TaskTarget.of(Material.COOKED_RABBIT),
                    TaskTarget.of(Material.COOKED_COD),
                    TaskTarget.of(Material.COOKED_SALMON),
                    TaskTarget.of(Material.APPLE),
                    TaskTarget.of(Material.GOLDEN_APPLE),
                    TaskTarget.of(Material.MELON_SLICE),
                    TaskTarget.of(Material.COOKIE),
                    TaskTarget.of(Material.PUMPKIN_PIE),
                    TaskTarget.of(Material.SWEET_BERRIES),
                    TaskTarget.of(Material.GLOW_BERRIES),
                    TaskTarget.of(Material.DRIED_KELP),
                    TaskTarget.of(Material.CARROT),
                    TaskTarget.of(Material.BAKED_POTATO),
                    TaskTarget.of(Material.MUSHROOM_STEW),
                    TaskTarget.of(Material.RABBIT_STEW),
                    TaskTarget.of(Material.BEETROOT_SOUP),
                    TaskTarget.of(Material.HONEY_BOTTLE)
            ), 4, 12),

            // ---- Crafting (CraftItemEvent; key = result Material) ----
            new TaskTemplate(TaskCategory.CRAFTING, "Craft {amount} {target}", List.of(
                    TaskTarget.of(Material.TORCH),
                    TaskTarget.of(Material.STICK),
                    TaskTarget.of(Material.CHEST),
                    TaskTarget.of(Material.OAK_PLANKS),
                    TaskTarget.of(Material.BREAD),
                    TaskTarget.of(Material.IRON_BLOCK),
                    TaskTarget.of(Material.LADDER),
                    TaskTarget.of(Material.CRAFTING_TABLE),
                    TaskTarget.of(Material.FURNACE),
                    TaskTarget.of(Material.OAK_DOOR),
                    TaskTarget.of(Material.OAK_FENCE),
                    TaskTarget.of(Material.OAK_STAIRS),
                    TaskTarget.of(Material.OAK_SLAB),
                    TaskTarget.of(Material.GLASS_PANE),
                    TaskTarget.of(Material.BOOKSHELF),
                    TaskTarget.of(Material.PAPER),
                    TaskTarget.of(Material.BOOK),
                    TaskTarget.of(Material.BUCKET),
                    TaskTarget.of(Material.ARROW),
                    TaskTarget.of(Material.COOKIE),
                    TaskTarget.of(Material.CAMPFIRE),
                    TaskTarget.of(Material.LANTERN),
                    TaskTarget.of(Material.ITEM_FRAME),
                    TaskTarget.of(Material.OAK_SIGN),
                    TaskTarget.of(Material.SCAFFOLDING),
                    TaskTarget.of(Material.GOLD_BLOCK)
            ), 5, 16),

            // ---- Cooking (FurnaceExtractEvent, food results) ----
            new TaskTemplate(TaskCategory.COOKING, "Cook {amount} {target}", List.of(
                    TaskTarget.of(Material.COOKED_BEEF),
                    TaskTarget.of(Material.COOKED_CHICKEN),
                    TaskTarget.of(Material.COOKED_PORKCHOP),
                    TaskTarget.of(Material.COOKED_MUTTON),
                    TaskTarget.of(Material.COOKED_RABBIT),
                    TaskTarget.of(Material.COOKED_COD),
                    TaskTarget.of(Material.COOKED_SALMON),
                    TaskTarget.of(Material.BAKED_POTATO),
                    TaskTarget.of(Material.DRIED_KELP)
            ), 5, 16),

            // ---- Smelting (FurnaceExtractEvent, non-food results) ----
            new TaskTemplate(TaskCategory.SMELTING, "Smelt {amount} {target}", List.of(
                    TaskTarget.of(Material.IRON_INGOT),
                    TaskTarget.of(Material.GOLD_INGOT),
                    TaskTarget.of(Material.COPPER_INGOT),
                    TaskTarget.of(Material.GLASS),
                    TaskTarget.of(Material.CHARCOAL),
                    TaskTarget.of(Material.STONE),
                    TaskTarget.of(Material.SMOOTH_STONE),
                    TaskTarget.of(Material.BRICK),
                    TaskTarget.of(Material.TERRACOTTA),
                    TaskTarget.of(Material.GREEN_DYE),
                    TaskTarget.of(Material.NETHERITE_SCRAP),
                    TaskTarget.of(Material.POPPED_CHORUS_FRUIT)
            ), 5, 16),

            // ---- Throwing (ProjectileLaunchEvent, shooter is player; key = projectile EntityType name) ----
            new TaskTemplate(TaskCategory.THROWING, "Throw {amount} {target}", List.of(
                    TaskTarget.keyed("Snowballs", "SNOWBALL"),
                    TaskTarget.keyed("Eggs", "EGG"),
                    TaskTarget.keyed("Ender Pearls", "ENDER_PEARL"),
                    TaskTarget.keyed("Tridents", "TRIDENT")
            ), 3, 10),

            // ---- Enchanting (GardensEnchantItemEvent; any item) ----
            new TaskTemplate(TaskCategory.ENCHANTING, "Enchant {amount} {target}", List.of(
                    TaskTarget.any("items")
            ), 2, 6),

            // ---- Foraging: collect nature blocks (BlockBreakEvent; key = block Material name) ----
            new TaskTemplate(TaskCategory.FORAGING, "Collect {amount} {target}", List.of(
                    TaskTarget.of("Logs",
                            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
                            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG),
                    TaskTarget.of("Leaves",
                            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES, Material.JUNGLE_LEAVES,
                            Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES),
                    TaskTarget.of("Flowers",
                            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
                            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
                            Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY),
                    TaskTarget.of("Mushrooms", Material.RED_MUSHROOM, Material.BROWN_MUSHROOM),
                    TaskTarget.of("Sugar Cane", Material.SUGAR_CANE),
                    TaskTarget.of("Bamboo", Material.BAMBOO),
                    TaskTarget.of("Cactus", Material.CACTUS),
                    TaskTarget.of("Sweet Berries", Material.SWEET_BERRY_BUSH),
                    TaskTarget.of("Pumpkins", Material.PUMPKIN),
                    TaskTarget.of("Melons", Material.MELON),
                    TaskTarget.of("Vines", Material.VINE),
                    TaskTarget.of("Glow Lichen", Material.GLOW_LICHEN),
                    TaskTarget.of("Moss", Material.MOSS_BLOCK, Material.MOSS_CARPET),
                    TaskTarget.of("Azalea", Material.AZALEA, Material.FLOWERING_AZALEA),
                    TaskTarget.of("Lily Pads", Material.LILY_PAD),
                    TaskTarget.of("Kelp", Material.KELP, Material.KELP_PLANT)
            ), 8, 32),

            // ---- Location: visit a biome (PlayerMoveEvent; key = full biome key "namespace:path") ----
            new TaskTemplate(TaskCategory.LOCATION, "Visit a {target} biome", List.of(
                    // vanilla biomes
                    TaskTarget.keyed("desert", "minecraft:desert"),
                    TaskTarget.keyed("jungle", "minecraft:jungle"),
                    TaskTarget.keyed("savanna", "minecraft:savanna"),
                    TaskTarget.keyed("plains", "minecraft:plains"),
                    TaskTarget.keyed("snowy plains", "minecraft:snowy_plains"),
                    TaskTarget.keyed("swamp", "minecraft:swamp"),
                    TaskTarget.keyed("taiga", "minecraft:taiga"),
                    TaskTarget.keyed("forest", "minecraft:forest"),
                    TaskTarget.keyed("birch forest", "minecraft:birch_forest"),
                    TaskTarget.keyed("dark forest", "minecraft:dark_forest"),
                    TaskTarget.keyed("badlands", "minecraft:badlands"),
                    TaskTarget.keyed("flower forest", "minecraft:flower_forest"),
                    TaskTarget.keyed("cherry grove", "minecraft:cherry_grove"),
                    TaskTarget.keyed("mangrove swamp", "minecraft:mangrove_swamp"),
                    TaskTarget.keyed("bamboo jungle", "minecraft:bamboo_jungle"),
                    TaskTarget.keyed("mushroom fields", "minecraft:mushroom_fields"),
                    TaskTarget.keyed("ocean", "minecraft:ocean"),
                    TaskTarget.keyed("beach", "minecraft:beach"),
                    // custom Gardens biomes
                    TaskTarget.keyed("coniferous forest", "gardens:coniferous_forest"),
                    TaskTarget.keyed("ebony forest", "gardens:ebony_forest"),
                    TaskTarget.keyed("eucalyptus forest", "gardens:eucalyptus_forest"),
                    TaskTarget.keyed("jacaranda grove", "gardens:jacaranda_grove"),
                    TaskTarget.keyed("maple forest", "gardens:maple_forest"),
                    TaskTarget.keyed("palm beach", "gardens:palm_beach"),
                    TaskTarget.keyed("redwood taiga", "gardens:redwood_taiga"),
                    TaskTarget.keyed("wisteria grove", "gardens:wisteria_grove"),
                    // Nether biomes
                    TaskTarget.keyed("crimson forest", "minecraft:crimson_forest"),
                    TaskTarget.keyed("warped forest", "minecraft:warped_forest"),
                    // End biomes
                    TaskTarget.keyed("end highlands", "minecraft:end_highlands"),
                    TaskTarget.keyed("end midlands", "minecraft:end_midlands")
            ), 1, 1)
    );
}
