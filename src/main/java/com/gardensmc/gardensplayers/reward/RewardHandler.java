package com.gardensmc.gardensplayers.reward;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerRewardsCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.RewardEntry;
import com.gardensmc.gardensplayers.util.DataFiles;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RewardHandler {

    private static List<Reward> rewards;

    public static void loadRewards() {
        var loaded = new ArrayList<Reward>();
        for (var key : DataFiles.rewardsYaml.getKeys(false)) {
            try {
                loaded.add(new Reward(key, Objects.requireNonNull(DataFiles.rewardsYaml.getConfigurationSection(key))));
            } catch (Exception e) {
                // e.g. an ItemEdit item that doesn't exist, or a malformed expiration date
                GardensPlayers.plugin.getLogger().severe("Skipping reward '" + key + "': " + e);
            }
        }
        rewards = List.copyOf(loaded);
    }

    public static CompletableFuture<Void> grantPendingRewards(Player player) {
        var currentRewards = rewards.stream()
                .filter(reward -> reward.getExpiration() == null || Instant.now().isBefore(reward.getExpiration()))
                .filter(reward -> reward.getPermission() == null || player.hasPermission(reward.getPermission()))
                .toList();
        return CompletableFuture.supplyAsync(() -> {
            boolean grantedReward = false;
            for (Reward reward : currentRewards) {
                var hasReward = Tables.rewardsTable.hasReward(player.getUniqueId().toString(), reward.getId());
                if (!hasReward) {
                    Tables.rewardsTable.giveReward(player.getUniqueId().toString(), makeRewardEntry(reward));
                    grantedReward = true;
                }
            }
            return grantedReward;
        }).thenAccept(grantedReward -> PlayerRewardsCache.INSTANCE.remove(player.getUniqueId()));
    }

    public static RewardEntry makeRewardEntry(Reward reward) {
        return new RewardEntry(reward.getId(), reward.getItemNBT(), false);
    }
}
