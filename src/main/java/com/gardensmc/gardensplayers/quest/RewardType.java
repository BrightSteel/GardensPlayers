package com.gardensmc.gardensplayers.quest;

import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The kinds of reward a quest task can grant. Gems are intentionally rare, so their range is small; acorns and XP
 * are common and use larger ranges.
 */
@Getter
public enum RewardType {

    XP(50, 500, "<green>XP") {
        @Override
        public void grant(Player player, int amount) {
            player.giveExp(amount);
        }
    },
    GEMS(1, 3, GemsCurrencyHandler.INSTANCE.symbol()) {
        @Override
        public void grant(Player player, int amount) {
            GemsCurrencyHandler.INSTANCE.addCurrency(player, amount);
        }
    },
    ACORNS(50, 300, AcornsCurrencyHandler.INSTANCE.symbol()) {
        @Override
        public void grant(Player player, int amount) {
            AcornsCurrencyHandler.INSTANCE.addCurrency(player, amount);
        }
    };

    private final int min;
    private final int max;
    /**
     * MiniMessage label/symbol shown next to the amount in lore and chat (e.g. the gems glyph or "XP").
     */
    private final String label;

    RewardType(int min, int max, String label) {
        this.min = min;
        this.max = max;
        this.label = label;
    }

    public int randomAmount() {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** The amount for a task this hard (0 the easiest its kind asks, 1 the hardest), to the nearest 5. */
    public int amountFor(double difficulty) {
        double clamped = Math.max(0, Math.min(1, difficulty));
        return (int) Math.round((min + (max - min) * clamped) / 5) * 5;
    }

    /**
     * Grants {@code amount} of this reward to {@code player}. Must be called on the main thread.
     */
    public abstract void grant(Player player, int amount);
}
