package com.gardensmc.gardensplayers.cache;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerEntryCache extends AsyncLoadingCache<UUID, PlayerEntry> {

    public static final PlayerEntryCache INSTANCE = new PlayerEntryCache();

    @Override
    protected CompletableFuture<PlayerEntry> createCacheEntry(UUID key) {
        return CompletableFuture.supplyAsync(() -> {
            var playerEntry = Tables.playerTable.getPlayerEntry(key);
            return Objects.requireNonNullElseGet(
                    playerEntry,
                    () -> createPlayerEntry(key)
            );
        });
    }

    private PlayerEntry createPlayerEntry(UUID uuid) {
        var player = Objects.requireNonNull(Bukkit.getPlayer(uuid));
        Tables.playerTable.createPlayerEntry(player);
        Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            // announce new player
            Bukkit.getServer().sendMessage(Component.text()
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("Welcome "))
                    .append(player.displayName())
                    .append(Component.text(" to Gardens MC"))
                    .build()
            );
            // force them to spawn - huskhomes doesn't do this :(
            player.chat("/spawn");
        });
        return Tables.playerTable.getPlayerEntry(uuid);
    }
}
