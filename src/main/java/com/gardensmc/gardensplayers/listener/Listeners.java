package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.event.Listener;

public class Listeners {

    public static void registerListeners() {
        register(new PlayerListener());
        register(new FurnitureListener());
        register(new CropsListener());
        register(new WorldListener());
        register(new BlockListener());
        register(new DepositGuiListener());
        register(new BackpackListener());
        register(new SpawnLimitListener());
        register(new QuestProgressListener());
        register(new DeathListener());
        PlayerListener.registerPacketListener();
    }

    private static void register(Listener listener) {
        GardensPlayers.plugin.getServer().getPluginManager().registerEvents(listener, GardensPlayers.plugin);
    }

}
