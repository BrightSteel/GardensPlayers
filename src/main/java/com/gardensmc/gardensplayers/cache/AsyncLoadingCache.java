package com.gardensmc.gardensplayers.cache;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AsyncLoadingCache<K, V> {

    private final com.github.benmanes.caffeine.cache.AsyncLoadingCache<K, V> cache;

    public AsyncLoadingCache() {
        this(5);
    }

    public AsyncLoadingCache(int expirationMinutes) {
        cache = Caffeine.newBuilder()
                .expireAfterAccess(expirationMinutes, TimeUnit.MINUTES)
                .buildAsync((key, executor) -> createCacheEntry(key));
    }

    public CompletableFuture<V> get(K key) {
        return cache.get(key);
    }

    public V getSync(K key) {
        return cache.synchronous().get(key);
    }

    public void remove(K key) {
        cache.synchronous().invalidate(key);
    }

    public void removeAll() {
        cache.synchronous().invalidateAll();
    }

    protected abstract CompletableFuture<V> createCacheEntry(K key);

}
