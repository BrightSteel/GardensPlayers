package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.shop.ShopDay;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.ShopLimitEntry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caches all shop entries for a given player on current day only.
 */
public class ShopLimitsCache extends AsyncLoadingCache<UUID, Map<String, ShopLimitEntry>> {

    public static final ShopLimitsCache INSTANCE = new ShopLimitsCache();

    private ShopLimitsCache() {
        super(2);
    }

    @Override
    protected CompletableFuture<Map<String, ShopLimitEntry>> createCacheEntry(UUID key) {
        return CompletableFuture.supplyAsync(() -> Tables.shopLimitsTable.getShopLimits(key, ShopDay.today())
            .stream()
            .collect(Collectors.toMap(ShopLimitEntry::getShopItem, Function.identity())));
    }

}
