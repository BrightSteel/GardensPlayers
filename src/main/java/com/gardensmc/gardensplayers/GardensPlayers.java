package com.gardensmc.gardensplayers;

import com.gardensmc.gardensplayers.wardrobe.WardrobeBridge;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.command.Commands;
import com.gardensmc.gardensplayers.config.Config;
import com.gardensmc.gardensplayers.database.DatabaseConnector;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.economy.GardensEconomyProvider;
import com.gardensmc.gardensplayers.listener.Listeners;
import com.gardensmc.gardensplayers.menu.shop.Shop;
import com.gardensmc.gardensplayers.menu.shop.ShopOrder;
import com.gardensmc.gardensplayers.menu.tile.TileMenuHandler;
import com.gardensmc.gardensplayers.placeholder.GardensPlayersPlaceholder;
import com.gardensmc.gardensplayers.quest.QuestHandler;
import com.gardensmc.gardensplayers.resourceworld.GenResourceHandler;
import com.gardensmc.gardensplayers.restart.RestartHandler;
import com.gardensmc.gardensplayers.reward.RewardHandler;
import com.gardensmc.gardensplayers.rtp.RtpHandler;
import com.gardensmc.gardensplayers.spawnlimit.SpawnLimits;
import com.gardensmc.gardensplayers.util.DataFiles;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.chunky.api.ChunkyAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GardensPlayers extends JavaPlugin {

    public static GardensPlayers plugin;
    public static Economy economy;
    public static Permission permission;
    public static Config config;
    public static List<Shop> shops;
    public static SpawnLimits spawnLimits;
    public static ChunkyAPI chunkyAPI;

    @Override
    public void onLoad() {
        // the Vault provider registered below can be called by other plugins before our onEnable
        plugin = this;
        // load economy
        try {
            economy = new GardensEconomyProvider();
            // Vault registers their Essentials provider at low priority, so we have to use normal priority here
            Class.forName("net.milkbowl.vault.economy.Economy");
            getServer().getServicesManager().register(
                    net.milkbowl.vault.economy.Economy.class,
                    economy,
                    this,
                    ServicePriority.Normal
            );
        } catch (final ClassNotFoundException ignored) {
            // Probably safer than fetching for the plugin as bukkit may not have marked it as enabled at this point in time
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        DataFiles.loadYamls();
        com.gardensmc.gardensplayers.streak.StreakHandler.INSTANCE.reload();
        com.gardensmc.gardensplayers.title.Titles.INSTANCE.reload();
        config = new Config(getConfig());
        Tables.initializeClass();
        Listeners.registerListeners();
        WardrobeBridge.enable(this);
        Commands.registerCommands();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GardensPlayersPlaceholder().register();
        }
        if (Bukkit.getPluginManager().getPlugin("EnderMap") != null) {
            com.gardensmc.gardensplayers.map.EnderMapWarps.register(this);   // public warps on the web map
        }

        // register vault instances
        verifyVaultInstalled();
        permission = getServiceProvider(Permission.class);
        try {
            loadShops();
        } catch (Exception e) {
            // keep enabling: the AFK tick, RTP queue, quests and daily restart below must still be scheduled
            getLogger().log(Level.SEVERE, "Failed to load shops/rewards - fix the config and run /gardensplayers reload", e);
        }
        chunkyAPI = Bukkit.getServer().getServicesManager().load(ChunkyAPI.class);

        int tickEvery = 2;
        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> Bukkit.getOnlinePlayers().forEach(player -> tickPlayer(player, tickEvery)),
                0,
                tickEvery
        );
        Bukkit.getScheduler().runTaskTimer(this, RtpHandler.INSTANCE::queueLocations, 20, 60);
        // update cache periodically to update visits in case cache hasn't updated for other reasons
        Bukkit.getScheduler().runTaskTimer(this, WarpsHandler.INSTANCE::clearCachedWarps, 20, 6000);
        Bukkit.getScheduler().runTaskTimer(this, GenResourceHandler.INSTANCE::tick, 20, 40);
        // the resource worlds are ours to load (not Multiverse's), and staff should hear early if replacements run low
        var storage = com.gardensmc.gardensplayers.resourceworld.ResourceWorldStorage.INSTANCE;
        var reset = com.gardensmc.gardensplayers.resourceworld.ResetResourceHandler.INSTANCE;
        storage.cleanUpPartialDownloads();
        reset.loadWorlds();
        storage.refresh();
        storage.uploadFinished();
        storage.fillLocal();
        // once storage has been listed: warn if running low, then reset if this month's is due (startup is right after
        // the midnight restart, so nobody's online), and check again every hour
        storage.afterQueued(() -> {
            reset.checkSupply();
            reset.checkMonthly();
        });
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            storage.refresh();
            storage.afterQueued(reset::checkMonthly);
        }, 72000, 72000);

        // load persisted quest state on startup, then regenerate at each midnight (America/Los_Angeles) and autosave every 5 minutes
        if (!QuestHandler.INSTANCE.loadFromFile()) {
            QuestHandler.INSTANCE.generateDailyTasks();
        }
        QuestHandler.INSTANCE.scheduleNextReset();
        QuestHandler.INSTANCE.scheduleAutoSave();

        // schedule the daily server restart at midnight (America/Los_Angeles) with countdown warnings
        if (config.isDailyRestartEnabled()) {
            RestartHandler.INSTANCE.scheduleNextRestart();
        }

        // players already online (a plugin reload): load their streaks and titles as if they'd just joined
        for (var player : Bukkit.getOnlinePlayers()) {
            com.gardensmc.gardensplayers.streak.StreakHandler.INSTANCE.onJoin(player);
            com.gardensmc.gardensplayers.title.Titles.INSTANCE.load(player.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        // persist quest state synchronously — async tasks won't run during shutdown
        QuestHandler.INSTANCE.save();
        // Database writes (quit saves, money changes) run on the common pool as daemon tasks, which the JVM
        // drops at exit - wait for them before closing the pool, or every restart loses the last writes.
        if (!ForkJoinPool.commonPool().awaitQuiescence(10, TimeUnit.SECONDS)) {
            getLogger().warning("Some database writes were still running at shutdown");
        }
        try {
            DatabaseConnector.INSTANCE.close();
        } catch (Throwable e) {
            getLogger().log(Level.WARNING, "Failed to close the database pool", e);
        }
    }

    private void tickPlayer(Player player, int ticksPassed) {
        OnlinePlayerCache.INSTANCE.getOnlinePlayerData(player.getUniqueId()).tick(player, ticksPassed);
    }

    private void verifyVaultInstalled() {
        var vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null) {
            getLogger().severe("Vault is not installed, not able to register economy!");
            disablePlugin();
        }
    }

    private <T> T getServiceProvider(Class<T> provider) {
        RegisteredServiceProvider<T> rsp = getServer().getServicesManager().getRegistration(provider);
        if (rsp == null) {
            getLogger().severe("Failed to get service provider " + provider.getSimpleName());
            disablePlugin();
            return null; // disabling should just halt code so this won't be null...
        }
        return rsp.getProvider();
    }

    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    public static void loadShops() {
        var shopsDir = new File(GardensPlayers.plugin.getDataFolder(), "shops");
        if (!shopsDir.exists()) {
            shopsDir.mkdir();
        }
        var orderFile = new File(shopsDir, ShopOrder.FILE);
        var shopFiles = shopsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml") && !name.equalsIgnoreCase(ShopOrder.FILE));
        if (shopFiles != null) {
            var loaded = new ArrayList<Shop>();
            for (var shopFile : shopFiles) {
                try {
                    var id = shopFile.getName().substring(0, shopFile.getName().length() - ".yml".length());
                    loaded.add(new Shop(id, YamlConfiguration.loadConfiguration(shopFile)));
                } catch (Exception e) {
                    GardensPlayers.plugin.getLogger().severe("Skipping shop file " + shopFile.getName() + ": " + e);
                }
            }
            // in shops/order.yml's order; any it doesn't list after, by name
            var wanted = orderFile.exists() ? YamlConfiguration.loadConfiguration(orderFile).getStringList("order") : List.<String>of();
            shops = ShopOrder.sort(loaded, Shop::getId, Shop::getName, wanted);
        }
        spawnLimits = new SpawnLimits(DataFiles.spawnLimitsYaml);
        RewardHandler.loadRewards();
        TileMenuHandler.constructShopPages();
    }
}
