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
    /** Reset the resource worlds once a month, on this day (Los Angeles time), or never if disabled. */
    private final boolean resourceResetEnabled;
    private final int resourceResetDay;
    /** Remote storage (Cloudflare R2, or any S3-compatible store) for pregenerated resource worlds. */
    private final boolean storageEnabled;
    private final String storageEndpoint;
    private final String storageBucket;
    private final String storageAccessKey;
    private final String storageSecretKey;
    private final String storageRegion;
    private final String storagePrefix;
    /** The generating machine: upload each finished world, then delete it locally. */
    private final boolean storageUploadGenerated;
    /** A server that resets: keep this many sets downloaded ahead. */
    private final int storageKeepLocal;

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
        resourceResetEnabled = configuration.getBoolean("resourceWorlds.reset.enabled", true);
        resourceResetDay = Math.max(1, Math.min(28, configuration.getInt("resourceWorlds.reset.day-of-month", 1)));
        storageEnabled = configuration.getBoolean("resourceWorlds.storage.enabled", false);
        storageEndpoint = configuration.getString("resourceWorlds.storage.endpoint", "");
        storageBucket = configuration.getString("resourceWorlds.storage.bucket", "");
        storageAccessKey = configuration.getString("resourceWorlds.storage.access-key", "");
        storageSecretKey = configuration.getString("resourceWorlds.storage.secret-key", "");
        storageRegion = configuration.getString("resourceWorlds.storage.region", "auto");
        storagePrefix = configuration.getString("resourceWorlds.storage.prefix", "resource-worlds/");
        storageUploadGenerated = configuration.getBoolean("resourceWorlds.storage.upload-generated", false);
        storageKeepLocal = Math.max(0, configuration.getInt("resourceWorlds.storage.keep-local", 1));
    }
}
