package com.gardensmc.gardensplayers.quest;

import org.bukkit.Material;

import java.util.Set;

/**
 * A possible subject of a task template, e.g. "Diamond Ore" matching both {@code DIAMOND_ORE} and
 * {@code DEEPSLATE_DIAMOND_ORE}. An empty {@link #matchKeys()} set means "any valid subject in the category"
 * (the category's listener restricts what counts). Menu icons are per-category, so targets carry no icon.
 *
 * @param displayName human-readable name used in task descriptions
 * @param matchKeys   normalized keys (Material/EntityType {@code name()}, custom crop/loot name, or biome key)
 *                    this target counts; empty = any
 */
public record TaskTarget(String displayName, Set<String> matchKeys) {

    /**
     * A target matching a single Material, named after it.
     */
    public static TaskTarget of(Material material) {
        return new TaskTarget(readable(material.name()), Set.of(material.name()));
    }

    /**
     * A named target matching several Materials.
     */
    public static TaskTarget of(String displayName, Material... materials) {
        return new TaskTarget(displayName, namesOf(materials));
    }

    /**
     * A named target matching arbitrary keys (e.g. EntityType names, custom loot/biome ids).
     */
    public static TaskTarget keyed(String displayName, String... keys) {
        return new TaskTarget(displayName, Set.of(keys));
    }

    /**
     * An "any" target for the category (matches every subject the category's listener forwards).
     */
    public static TaskTarget any(String displayName) {
        return new TaskTarget(displayName, Set.of());
    }

    private static Set<String> namesOf(Material... materials) {
        var names = new java.util.HashSet<String>();
        for (Material material : materials) {
            names.add(material.name());
        }
        return names;
    }

    private static String readable(String enumName) {
        var parts = enumName.toLowerCase().split("_");
        var sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
