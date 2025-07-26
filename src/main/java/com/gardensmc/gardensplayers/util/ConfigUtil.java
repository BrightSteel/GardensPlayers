package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigUtil {

    public static YamlConfiguration furnitureShopsYaml;

    public static void loadYamls() {
        File dayFile = getYMLFile("furniture_shops.yml");
        furnitureShopsYaml = YamlConfiguration.loadConfiguration(dayFile);
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
