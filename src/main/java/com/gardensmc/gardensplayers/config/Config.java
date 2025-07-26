package com.gardensmc.gardensplayers.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class Config {

    private final String currencySymbol;
    private final int startingBalance;
    private final String messagePrefix;
    private final String errorColor;
    private final String mainColor;

    public Config(FileConfiguration configuration) {
        var economy = configuration.getConfigurationSection("economy");
        currencySymbol = economy.getString("currencySymbol");
        startingBalance = economy.getInt("startingBalance");
        var messages = configuration.getConfigurationSection("messages");
        messagePrefix = messages.getString("prefix");
        errorColor = messages.getString("errorColor");
        mainColor = messages.getString("mainColor");
    }
}
