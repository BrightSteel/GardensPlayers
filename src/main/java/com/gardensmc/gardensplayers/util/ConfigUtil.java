package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigUtil {

    public static YamlConfiguration furnitureShopsYaml;
    public static YamlConfiguration rewardsYaml;
    public static YamlConfiguration spawnLimitsYaml;

    public static void loadYamls() {
        File dayFile = getYMLFile("furniture_shops.yml");
        furnitureShopsYaml = YamlConfiguration.loadConfiguration(dayFile);
        rewardsYaml = YamlConfiguration.loadConfiguration(getYMLFile("rewards.yml"));
        spawnLimitsYaml = YamlConfiguration.loadConfiguration(getYMLFile("spawn_limits.yml"));
    }

    public static File getYMLFile(String file_name) {
        File file = new File(GardensPlayers.plugin.getDataFolder(), file_name);
        try {
            file.getParentFile().mkdirs();
            file.createNewFile(); // create file if not exists
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
}
