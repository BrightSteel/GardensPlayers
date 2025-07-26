package com.gardensmc.gardensplayers.shop;

import com.gardensmc.gardenscrops.GardensCrops;
import com.gardensmc.gardensplayers.cache.ShopLimitKey;
import com.gardensmc.gardensplayers.cache.ShopLimitsCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.ShopLimitEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class ShopLimitHandler {

    private final UUID playerUUID;
    private final String shopItem;
    @Getter
    private final int limit;

    @Nullable
    private ShopLimitEntry getShopLimitEntry() {
        var shopLimitEntries = ShopLimitsCache.INSTANCE.getSync(playerUUID);
        return shopLimitEntries.get(shopItem);
    }

    public int getAmountLeft() {
        var shopLimitEntry = getShopLimitEntry();
        var currentAmount = shopLimitEntry == null ? 0 : shopLimitEntry.getAmount();
        return limit - currentAmount;
    }

    public boolean isLimitMet() {
        var shopLimitEntry = getShopLimitEntry();
        if (shopLimitEntry == null) {
            return false;
        }
        return shopLimitEntry.getAmount() == limit;
    }

    public boolean isTransactionWithinLimit(int amount) {
        var shopLimitEntry = getShopLimitEntry();
        var currentAmount = shopLimitEntry == null ? 0 : shopLimitEntry.getAmount();
        return currentAmount + amount <= limit;
    }

    public CompletableFuture<Void> increaseAmountTransacted(int increase) {
        var currentDay = GardensCrops.getCurrentDay();
        var shopLimitKey = new ShopLimitKey(playerUUID, shopItem);
        return CompletableFuture.runAsync(() -> {
            var shopLimitEntry = Tables.shopLimitsTable.getShopLimit(shopLimitKey);
            if (shopLimitEntry == null) {
                Tables.shopLimitsTable.createShopLimit(shopLimitKey, increase, currentDay);
            } else {
                var currentAmount = currentDay == shopLimitEntry.getDay() ? shopLimitEntry.getAmount() : 0;
                Tables.shopLimitsTable.updateShopLimit(
                        shopLimitKey,
                        currentAmount + increase,
                        currentDay
                );
            }
        }).thenRun(() -> ShopLimitsCache.INSTANCE.remove(playerUUID));
    }
}
