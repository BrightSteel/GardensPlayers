package com.gardensmc.gardensplayers.spawnlimit;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class SpawnLimits {

    private final int radius;
    private int spawnerLimit = -1;
    private int breedLimit = -1;
    private final boolean isBreedDumbAtLimit;

    public SpawnLimits(ConfigurationSection configurationSection) {
        this.radius = configurationSection.getInt("radius", 8);
        var limitSection = configurationSection.getConfigurationSection("limits");
        if (limitSection != null) {
            this.spawnerLimit = limitSection.getInt("spawner");
            this.breedLimit = limitSection.getInt("breed");
        }
        this.isBreedDumbAtLimit = configurationSection.getBoolean("breed_dumb_at_limit");
    }
}
