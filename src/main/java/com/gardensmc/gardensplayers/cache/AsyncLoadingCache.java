package com.gardensmc.gardensplayers.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;

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

    // the value if it's already loaded, otherwise null (starting a load in the background) - never blocks
    @Nullable
    public V getIfLoaded(K key) {
        var future = cache.getIfPresent(key);
        if (future == null) {
            cache.get(key);
            return null;
        }
        return future.isDone() && !future.isCompletedExceptionally() ? future.join() : null;
    }

    // the value if it's already loaded, otherwise null - never blocks and never starts a load
    @Nullable
    protected V getIfPresentNow(K key) {
        var future = cache.getIfPresent(key);
        return future != null && future.isDone() && !future.isCompletedExceptionally() ? future.join() : null;
    }

    public void remove(K key) {
        cache.synchronous().invalidate(key);
    }

    public void removeAll() {
        cache.synchronous().invalidateAll();
    }

    protected abstract CompletableFuture<V> createCacheEntry(K key);

}
