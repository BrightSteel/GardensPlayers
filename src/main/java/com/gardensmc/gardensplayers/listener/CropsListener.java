package com.gardensmc.gardensplayers.listener;

import com.gardensmc.gardenscrops.event.DayCycleEvent;
import com.gardensmc.gardensplayers.cache.ShopLimitsCache;
import com.gardensmc.gardensplayers.shop.FarmShopMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CropsListener implements Listener {

    @EventHandler
    public void onDayCycle(DayCycleEvent e) {
        FarmShopMenu.resetBoostedCrop();
        ShopLimitsCache.INSTANCE.removeAll();
    }
}
