package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.GuildEntry;
import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Caches each guild's data (guild id -&gt; its entry and full member list), loading from the database on miss and
 * expiring 5 minutes after last access. Keyed by guild id so the shared member roster is stored once and a change to a
 * guild is a single {@code remove(guildId)} invalidation — no need to enumerate and evict every member.
 *
 * <p>To go from a player to their guild, resolve their guild id via {@link PlayerGuildCache} first.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GuildCache extends AsyncLoadingCache<Integer, GuildCache.CachedGuild> {

    public static final GuildCache INSTANCE = new GuildCache();

    @Override
    protected CompletableFuture<CachedGuild> createCacheEntry(Integer guildId) {
        return CompletableFuture.supplyAsync(() -> new CachedGuild(
                Tables.guildsTable.getGuildById(guildId),
                Tables.guildMembersTable.getMembers(guildId)));
    }

    /** A guild's cached data. {@link #guild()} is {@code null} only if the guild no longer exists. */
    public record CachedGuild(@Nullable GuildEntry guild, List<GuildMemberEntry> members) {
    }
}
