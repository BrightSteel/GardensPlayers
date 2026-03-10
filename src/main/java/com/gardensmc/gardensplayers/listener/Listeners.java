package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.backpack.BackpackHandler;
import org.bukkit.event.Listener;

public class Listeners {

    public static void registerListeners() {
        register(new PlayerListener());
        register(new FurnitureListener());
        register(new CropsListener());
//        register(new ShopGuiListener());
        register(new WorldListener());
        register(new BlockListener());
        register(new DepositGuiListener());
        register(new WardrobeGuiListener());
        register(new BackpackListener());
        register(new BackpackGuiListener());
        register(new SpawnLimitListener());
        BackpackHandler.registerUseListener();
        PlayerListener.registerPacketListener();
    }

    private static void register(Listener listener) {
        GardensPlayers.plugin.getServer().getPluginManager().registerEvents(listener, GardensPlayers.plugin);
    }

}
