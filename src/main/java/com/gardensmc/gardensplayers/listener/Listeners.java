package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.event.Listener;

public class Listeners {

    public static void registerListeners() {
        register(new PlayerListener());
        register(new FurnitureListener());
        register(new CropsListener());
    }

    private static void register(Listener listener) {
        GardensPlayers.plugin.getServer().getPluginManager().registerEvents(listener, GardensPlayers.plugin);
    }

}
