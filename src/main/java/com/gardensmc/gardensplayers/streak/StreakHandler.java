package com.gardensmc.gardensplayers.streak;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.quest.QuestHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login streaks: a day counts once a player finishes enough of the day's quest tasks (streaks.yml), or the starter
 * quest for a new player. Days in a row grow the streak and climb a reward ladder; savers cover missed days. A day
 * counts the moment it's earned and is saved straight away, so a crash or the midnight restart can't lose it.
 */
public final class StreakHandler {

    public static final StreakHandler INSTANCE = new StreakHandler();
    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");

    private StreakConfig config;
    private final Map<UUID, Streak> streaks = new ConcurrentHashMap<>();

    private StreakHandler() {
    }

    public void reload() {
        config = StreakConfig.load();
    }

    /** Whether streaks are on (streaks.yml enabled): off, nothing counts, pays or shows; staff commands still work. */
    public boolean enabled() {
        return config != null && config.enabled();
    }

    public StreakConfig config() {
        return config;
    }

    /** Today, as the epoch day in America/Los_Angeles (the quest board's day). */
    public static long today() {
        return LocalDate.now(ZONE).toEpochDay();
    }

    @Nullable
    public Streak cached(UUID player) {
        return streaks.get(player);
    }

    // ---- joining and leaving ----

    /** Loads the player's streak, catches it up to today and tells them where they stand. */
    public void onJoin(Player player) {
        if (!enabled()) return;
        var uuid = player.getUniqueId();
        PlayerEntryCache.INSTANCE.get(uuid)
                .thenApplyAsync(entry -> Tables.streakTable.get(uuid))
                .thenAccept(streak -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                    if (!player.isOnline()) return;
                    var settled = StreakRules.settle(streak, today());
                    streaks.put(uuid, streak);
                    if (settled.changed()) {
                        save(uuid, streak);
                        log(player, settled.ended() > 0 ? "lost a " + settled.ended() + "-day streak" : "used " + settled.saversUsed() + " saver(s)");
                    }
                    // after the quest reminder, so the two read in order
                    Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> {
                        if (!player.isOnline()) return;
                        if (settled.ended() > 0) {
                            send(player, "<gray>Your <white>" + settled.ended() + "-day<gray> streak ended. Your best is <white>"
                                    + streak.best() + " days<gray>. Finish " + config.tasksPerDay() + " daily tasks to start a new one.");
                        } else if (settled.saversUsed() > 0) {
                            send(player, "<gold>A streak saver covered " + (settled.saversUsed() == 1 ? "yesterday" : settled.saversUsed() + " missed days")
                                    + "<gray>, so your streak is safe. Savers left: <white>" + streak.savers());
                        }
                        send(player, status(player, streak));
                    }, 60);
                }));
    }

    public void onQuit(UUID player) {
        streaks.remove(player);
    }

    // ---- counting days ----

    /** A daily task was finished: count today once enough of them are. Main thread. */
    public void onTaskCompleted(Player player) {
        if (!enabled()) return;
        if (QuestHandler.INSTANCE.getCompletedToday(player.getUniqueId()) >= config.tasksPerDay()) {
            credit(player, "daily tasks");
        }
    }

    /** Counts today for the player, if it hasn't already. Main thread. False if it didn't count. */
    public boolean credit(Player player, String why) {
        if (!enabled()) return false;
        var uuid = player.getUniqueId();
        var streak = streaks.get(uuid);
        if (streak == null) return false;
        long today = today();
        StreakRules.settle(streak, today);
        int saversBefore = streak.savers();
        int day = StreakRules.count(streak, today, config.saverEvery(), config.maxSavers());
        if (day == 0) return false;

        var rewards = config.rewardsFor(day);
        var got = new ArrayList<String>();
        for (var reward : rewards) {
            reward.grant(player);
            got.add(reward.describe());
        }
        if (streak.savers() > saversBefore) got.add("a streak saver");

        player.sendActionBar(MiniMessage.miniMessage().deserialize("<gold>Streak day " + day + "!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        send(player, "<gold>Streak day " + day + "!" + (got.isEmpty() ? "" : " <gray>You got <white>" + String.join("<gray>, <white>", got) + "<gray>."));
        save(uuid, streak);
        log(player, "streak day " + day + " (" + why + ")");
        return true;
    }

    /** Sets a player's streak (staff), counting today. Main thread. */
    public void set(Player player, int days) {
        var uuid = player.getUniqueId();
        var streak = streaks.get(uuid);
        if (streak == null) return;
        streak.current = Math.max(0, days);
        streak.best = Math.max(streak.best, streak.current);
        streak.lastDay = days > 0 ? today() : -1;
        save(uuid, streak);
        log(player, "streak set to " + days + " by staff");
    }

    // ---- what players see ----

    /** One line: the streak, today's progress and the next reward. */
    public String status(Player player, Streak streak) {
        var line = new StringBuilder("<gold>Streak: <white>" + streak.current() + (streak.current() == 1 ? " day" : " days"));
        if (streak.countedOn(today())) {
            line.append(" <gray>· today counted <green>✔");
        } else {
            int left = Math.max(0, config.tasksPerDay() - QuestHandler.INSTANCE.getCompletedToday(player.getUniqueId()));
            line.append(" <gray>· finish <white>").append(left).append(left == 1 ? " more task" : " more tasks")
                    .append("<gray> today to make it <white>").append(streak.current() + 1);
        }
        var next = config.next(streak.current());
        if (next != null) {
            line.append(" <gray>· day <white>").append(next.getKey()).append("<gray>: ").append(describe(next.getValue()));
        }
        return line.toString();
    }

    /** The next few reward days after the player's current day. */
    public List<Map.Entry<Integer, List<StreakReward>>> upcoming(Streak streak, int count) {
        var list = new ArrayList<Map.Entry<Integer, List<StreakReward>>>();
        int day = streak.current();
        while (list.size() < count) {
            var next = config.next(day);
            if (next == null) break;
            list.add(next);
            day = next.getKey();
        }
        return list;
    }

    public static String describe(List<StreakReward> rewards) {
        var parts = new ArrayList<String>();
        for (var reward : rewards) parts.add(reward.describe());
        return "<white>" + String.join("<gray>, <white>", parts);
    }

    private static void send(Player player, String message) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    private static void save(UUID player, Streak streak) {
        var copy = streak.copy();
        CompletableFuture.runAsync(() -> Tables.streakTable.save(player, copy));
    }

    // a line per change, for the retention analysis to pick up from the logs
    private static void log(Player player, String what) {
        GardensPlayers.plugin.getLogger().info("[Streak] " + player.getName() + " " + what);
    }
}
