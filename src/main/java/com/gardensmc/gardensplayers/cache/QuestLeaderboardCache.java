package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.database.PlayerTable.QuestLeader;
import com.gardensmc.gardensplayers.database.Tables;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Caches the top quest completers (top 9, by lifetime quests completed). A single-entry cache that expires 2
 * minutes after each load, so the leaderboard can be briefly out of date but avoids querying the player table on
 * every menu open. The query runs off the main thread.
 */
public class QuestLeaderboardCache {

    public static final QuestLeaderboardCache INSTANCE = new QuestLeaderboardCache();

    // single-entry cache; the key is a constant
    private static final boolean KEY = true;

    private final AsyncLoadingCache<Boolean, List<QuestLeader>> cache;

    private QuestLeaderboardCache() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(Tables.playerTable::getTopQuestCompleters));
    }

    public List<QuestLeader> getSync() {
        return cache.synchronous().get(KEY);
    }
}
