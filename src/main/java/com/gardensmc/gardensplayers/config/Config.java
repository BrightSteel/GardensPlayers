package com.gardensmc.gardensplayers.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class Config {

    private final String currencySymbol;
    private final int startingBalance;
    private final int deathBackCost;
    private final String messagePrefix;
    private final String errorColor;
    private final String mainColor;
    private final int resourceWorldRadius;
    /** Whether /rtp works and spots are found ahead of time (which generates chunks; off on a dev server). */
    private final boolean rtpEnabled;

    public Config(FileConfiguration configuration) {
        var economy = configuration.getConfigurationSection("economy");
        currencySymbol = economy.getString("currencySymbol");
        startingBalance = economy.getInt("startingBalance");
        deathBackCost = economy.getInt("deathBackCost", 250);
        var messages = configuration.getConfigurationSection("messages");
        messagePrefix = messages.getString("prefix");
        errorColor = messages.getString("errorColor");
        mainColor = messages.getString("mainColor");
        resourceWorldRadius = configuration.getInt("resourceWorldRadius");
        rtpEnabled = configuration.getBoolean("rtp.enabled", true);
    }
}
