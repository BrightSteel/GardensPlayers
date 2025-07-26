package com.gardensmc.gardensplayers.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OnlinePlayerCache {

    public static final OnlinePlayerCache INSTANCE = new OnlinePlayerCache();
    private final LoadingCache<UUID, OnlinePlayerData> cache;

    private OnlinePlayerCache() {
        cache = CacheBuilder
                .newBuilder()
                // these are frequently accessed (ie tab placeholders) and thus can have a short expiration
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public OnlinePlayerData load(UUID key) {
                        return new OnlinePlayerData();
                    }
                });
    }

    public OnlinePlayerData getOnlinePlayerData(UUID key) {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
