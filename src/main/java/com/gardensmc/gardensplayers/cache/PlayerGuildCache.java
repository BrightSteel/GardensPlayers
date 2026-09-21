package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Index from a player to the id of the guild they belong to (player UUID -&gt; guild id, empty if none), loading from
 * the database on miss and expiring 5 minutes after last access. Pairs with {@link GuildCache} to resolve a player's
 * guild without a database hit per lookup.
 *
 * <p>Only a player's membership existence is cached here — not their role — so this only needs invalidating when the
 * player actually joins or leaves a guild, never on a rank change.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerGuildCache extends AsyncLoadingCache<UUID, Optional<Integer>> {

    public static final PlayerGuildCache INSTANCE = new PlayerGuildCache();

    @Override
    protected CompletableFuture<Optional<Integer>> createCacheEntry(UUID key) {
        return CompletableFuture.supplyAsync(() -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(key);
            return membership == null ? Optional.empty() : Optional.of(membership.getGuildId());
        });
    }
}
