package com.gardensmc.gardensplayers.crate;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public class CrateContent {

    private final String furnitureId;
    private final List<String> furnitureIds;

    public CrateContent(ConfigurationSection contentSection) {
        this.furnitureId = contentSection.getString("furniture_id");
        this.furnitureIds = contentSection.getStringList("furniture_ids");
    }
}
