package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardensplayers.cache.ShopLimitKey;
import com.gardensmc.gardensplayers.cache.ShopLimitsCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.ShopLimitEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class ShopLimitHandler {

    private final UUID playerUUID;
    private final String shopItem;
    @Getter
    private final int limit;

    // today's amount only: a cached entry from an earlier day no longer counts
    private int getAmountUsed() {
        var shopLimitEntry = ShopLimitsCache.INSTANCE.getSync(playerUUID).get(shopItem);
        if (shopLimitEntry == null || shopLimitEntry.getDay() != ShopDay.today()) {
            return 0;
        }
        return shopLimitEntry.getAmount();
    }

    public int getAmountLeft() {
        return Math.max(0, limit - getAmountUsed());
    }

    public boolean isLimitMet() {
        return getAmountLeft() <= 0;
    }

    public boolean isTransactionWithinLimit(int amount) {
        return amount <= getAmountLeft();
    }

    /**
     * Records `increase` against today's limit. The cached count is updated right away (shop clicks all run on
     * the main thread), so the very next sell sees it; the database catches up with one atomic upsert. The old
     * version only refreshed the cache after the async write, so two quick sells both passed the limit check.
     */
    public CompletableFuture<Void> increaseAmountTransacted(int increase) {
        var currentDay = ShopDay.today();
        var cached = ShopLimitsCache.INSTANCE.getSync(playerUUID);
        cached.put(shopItem, new ShopLimitEntry(shopItem, getAmountUsed() + increase, currentDay));
        var shopLimitKey = new ShopLimitKey(playerUUID, shopItem);
        return CompletableFuture.runAsync(() -> Tables.shopLimitsTable.addShopLimit(shopLimitKey, increase, currentDay));
    }
}
