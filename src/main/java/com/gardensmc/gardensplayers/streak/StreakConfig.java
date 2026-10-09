package com.gardensmc.gardensplayers.streak;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** streaks.yml: how many tasks make a day, savers, and the reward ladder. */
public record StreakConfig(boolean enabled, int tasksPerDay, int saverEvery, int maxSavers,
                           TreeMap<Integer, List<StreakReward>> ladder,
                           int repeatAfter, int repeatEvery, List<StreakReward> repeatRewards) {

    public static StreakConfig load() {
        var file = new File(GardensPlayers.plugin.getDataFolder(), "streaks.yml");
        if (!file.exists()) {
            GardensPlayers.plugin.saveResource("streaks.yml", false);
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        var ladder = new TreeMap<Integer, List<StreakReward>>();
        var rewards = yaml.getConfigurationSection("rewards");
        if (rewards != null) {
            for (var key : rewards.getKeys(false)) {
                try {
                    ladder.put(Integer.parseInt(key), parse(rewards.getStringList(key)));
                } catch (NumberFormatException e) {
                    GardensPlayers.plugin.getLogger().warning("streaks.yml: reward day '" + key + "' isn't a number");
                }
            }
        }
        return new StreakConfig(
                yaml.getBoolean("enabled", false),
                Math.max(1, yaml.getInt("tasks-per-day", 2)),
                yaml.getInt("savers.every", 7),
                yaml.getInt("savers.max", 2),
                ladder,
                yaml.getInt("repeat.after", 30),
                yaml.getInt("repeat.every", 7),
                parse(yaml.getStringList("repeat.rewards"))
        );
    }

    private static List<StreakReward> parse(List<String> lines) {
        var parsed = new ArrayList<StreakReward>();
        for (var line : lines) {
            var reward = StreakReward.parse(line);
            if (reward == null) {
                GardensPlayers.plugin.getLogger().warning("streaks.yml: can't read reward '" + line + "'");
            } else {
                parsed.add(reward);
            }
        }
        return parsed;
    }

    /** What reaching this streak day gives: its ladder rung, plus the repeating reward past the ladder's end. */
    public List<StreakReward> rewardsFor(int day) {
        var rewards = new ArrayList<>(ladder.getOrDefault(day, List.of()));
        if (repeatEvery > 0 && day > repeatAfter && (day - repeatAfter) % repeatEvery == 0) {
            rewards.addAll(repeatRewards);
        }
        return rewards;
    }

    /** The next day after {@code day} that gives something, or -1. */
    public int nextRewardDay(int day) {
        for (int d = day + 1; d <= day + Math.max(repeatEvery, 1) + ladder.lastKey(); d++) {
            if (!rewardsFor(d).isEmpty()) return d;
        }
        return -1;
    }

    public Map.Entry<Integer, List<StreakReward>> next(int day) {
        int d = nextRewardDay(day);
        return d < 0 ? null : Map.entry(d, rewardsFor(d));
    }
}
