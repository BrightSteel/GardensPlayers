//package com.gardensmc.gardensplayers.listener;
//
//import com.gardensmc.gardensplayers.GardensPlayers;
//import com.gardensmc.gardensplayers.economy.GemEconomyProvider;
//import net.brcdev.shopgui.ShopGuiPlusApi;
//import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;
//
//public class ShopGuiListener implements Listener {
//
//    @EventHandler
//    public void onShopPostEnable(ShopGUIPlusPostEnableEvent e) {
//        ShopGuiPlusApi.registerEconomyProvider(new GemEconomyProvider());
//        GardensPlayers.plugin.getLogger().info("Registered GardensGems economy provider in ShopGuiPlus");
//    }
//}
