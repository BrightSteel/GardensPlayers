package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Caches each player's accepted friends (player UUID -&gt; friend entries), loading from the database on miss.
 * Because an accepted friendship appears in both players' lists, invalidate <b>both</b> parties whenever a
 * relationship between them changes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FriendsCache extends AsyncLoadingCache<UUID, List<FriendEntry>> {

    public static final FriendsCache INSTANCE = new FriendsCache();

    @Override
    protected CompletableFuture<List<FriendEntry>> createCacheEntry(UUID key) {
        return CompletableFuture.supplyAsync(() -> Tables.friendsTable.getFriends(key));
    }
}
