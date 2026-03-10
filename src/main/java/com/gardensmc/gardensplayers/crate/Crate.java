package com.gardensmc.gardensplayers.crate;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class Crate {

    private final String furnitureId;
    private final String display;
    private final List<CrateContent> crateContents;

    public Crate(ConfigurationSection crateSection) {
        this.furnitureId = crateSection.getString("furniture_id");
        this.display = crateSection.getString("display");
        var crateContentsSection = crateSection.getConfigurationSection("contents");
        this.crateContents = crateContentsSection == null ? Collections.emptyList() : crateContentsSection.getKeys(false)
                .stream()
                .map(key -> new CrateContent(Objects.requireNonNull(crateContentsSection.getConfigurationSection(key))))
                .toList();
    }
}
