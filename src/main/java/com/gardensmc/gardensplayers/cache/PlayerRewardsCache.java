package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.RewardEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerRewardsCache extends AsyncLoadingCache<UUID, List<RewardEntry>> {

    public static final PlayerRewardsCache INSTANCE = new PlayerRewardsCache();

    @Override
    protected CompletableFuture<List<RewardEntry>> createCacheEntry(UUID key) {
        return CompletableFuture.supplyAsync(() -> Tables.rewardsTable.getUnclaimedRewards(key.toString()));
    }
}
