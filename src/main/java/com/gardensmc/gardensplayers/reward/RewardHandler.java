package com.gardensmc.gardensplayers.reward;

import com.gardensmc.gardensplayers.cache.PlayerRewardsCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.RewardEntry;
import com.gardensmc.gardensplayers.util.ConfigUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RewardHandler {

    private static List<Reward> rewards;

    public static void loadRewards() {
        rewards = ConfigUtil.rewardsYaml.getKeys(false)
                .stream()
                .map(key -> new Reward(key, Objects.requireNonNull(ConfigUtil.rewardsYaml.getConfigurationSection(key))))
                .toList();
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
