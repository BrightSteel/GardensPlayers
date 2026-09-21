package com.gardensmc.gardensplayers.quest;

import org.bukkit.entity.Player;

/**
 * A concrete reward attached to a generated {@link Task}: a {@link RewardType} and a fixed amount.
 */
public record TaskReward(RewardType type, int amount) {

    public static TaskReward random(RewardType type) {
        return new TaskReward(type, type.randomAmount());
    }

    public void grant(Player player) {
        type.grant(player, amount);
    }

    /**
     * MiniMessage description for menu lore / chat, e.g. {@code "<white>250 <green>XP"}.
     */
    public String describe() {
        return "<white>" + amount + " " + type.getLabel();
    }
}
