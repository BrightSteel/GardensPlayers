package com.gardensmc.gardensplayers;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.command.Commands;
import com.gardensmc.gardensplayers.config.Config;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.economy.GardensEconomyProvider;
import com.gardensmc.gardensplayers.listener.Listeners;
import com.gardensmc.gardensplayers.placeholder.GardensPlayersPlaceholder;
import com.gardensmc.gardensplayers.rtp.RtpHandler;
import com.gardensmc.gardensplayers.util.ConfigUtil;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class GardensPlayers extends JavaPlugin {

    public static GardensPlayers plugin;
    public static Economy economy;
    public static Chat chat;
    public static Permission permission;
    public static Config config;

    @Override
    public void onLoad() {
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
        ConfigUtil.loadYamls();
        config = new Config(getConfig());
        Tables.initializeClass();
        Listeners.registerListeners();
        Commands.registerCommands();
        if (Bukkit.getPluginManager().getPlugin("PlaceHolderAPI") != null) {
            new GardensPlayersPlaceholder().register();
        }

        // register vault instances
        verifyVaultInstalled();
//        economy = getServiceProvider(Economy.class);
//        chat = getServiceProvider(Chat.class);
        permission = getServiceProvider(Permission.class);

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
}
