package com.gardensmc.gardensplayers.quest;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.database.Tables;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Owns the daily quest state: the shared list of generated tasks for the day and each player's independent progress.
 * Everything is held in memory and reset when the daily tasks regenerate. The 16 tasks are shared by all players;
 * progress is tracked per player.
 */
public class QuestHandler {

    public static final QuestHandler INSTANCE = new QuestHandler();

    private static final int DAILY_TASK_COUNT = 16;
    // the whole day's task rewards can hand out at most this many gems in total
    private static final int MAX_DAILY_GEM_REWARD = 6;
    // daily tasks reset at midnight in this zone
    private static final ZoneId RESET_ZONE = ZoneId.of("America/Los_Angeles");

    @Getter
    private volatile List<Task> dailyTasks = List.of();
    @Getter
    private volatile Instant lastGenerated = Instant.EPOCH;

    // player -> (taskId -> progress)
    private final Map<UUID, Map<UUID, PlayerTaskProgress>> progress = new ConcurrentHashMap<>();

    private QuestHandler() {
    }

    /**
     * Generates a fresh set of {@value #DAILY_TASK_COUNT} daily tasks from random templates and clears all player
     * progress. Safe to call from the main thread on a timer.
     */
    public void generateDailyTasks() {
        var pool = new ArrayList<>(TaskTemplates.ALL);
        Collections.shuffle(pool);

        int gemBudget = MAX_DAILY_GEM_REWARD;
        var tasks = new ArrayList<Task>(DAILY_TASK_COUNT);
        for (int i = 0; i < DAILY_TASK_COUNT; i++) {
            // wrap around the shuffled pool if there are fewer templates than needed (categories may repeat)
            var template = pool.get(i % pool.size());
            var reward = rollReward(gemBudget);
            if (reward.type() == RewardType.GEMS) {
                gemBudget -= reward.amount();
            }
            tasks.add(template.generate(reward));
        }

        dailyTasks = List.copyOf(tasks);
        lastGenerated = Instant.now();
        progress.clear();
    }

    /**
     * Rolls a reward for a task, respecting the remaining daily gem budget. If gems come up but the budget is
     * exhausted, a non-gem reward is chosen instead; if only a little budget remains, the gem reward is capped to
     * it (so the last gem or two can still be awarded).
     */
    private TaskReward rollReward(int gemBudget) {
        var random = ThreadLocalRandom.current();
        var type = RewardType.values()[random.nextInt(RewardType.values().length)];
        if (type != RewardType.GEMS) {
            return TaskReward.random(type);
        }
        if (gemBudget <= 0) {
            // gem cap reached — award XP or acorns instead
            return TaskReward.random(random.nextBoolean() ? RewardType.XP : RewardType.ACORNS);
        }
        // don't let a single gem reward exceed the remaining daily budget
        return new TaskReward(RewardType.GEMS, Math.min(RewardType.GEMS.randomAmount(), gemBudget));
    }

    // ---- persistence ----

    private File dataFile() {
        return new File(GardensPlayers.plugin.getDataFolder(), "quest-data.yml");
    }

    /**
     * Schedules a periodic (every 5 minutes) async write of the in-memory quest state to disk.
     */
    public void scheduleAutoSave() {
        long fiveMinutes = 5L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(GardensPlayers.plugin, this::save, fiveMinutes, fiveMinutes);
    }

    /**
     * Writes quest state to disk on an async thread (use this from the main thread).
     */
    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(GardensPlayers.plugin, this::save);
    }

    /**
     * Writes the current daily tasks and all player progress to disk. Safe to call off the main thread.
     */
    // synchronized: completions now save right away, so saves can overlap and must not interleave writes
    public synchronized void save() {
        var yaml = new YamlConfiguration();
        yaml.set("lastGenerated", lastGenerated.toString());
        for (Task task : dailyTasks) {
            var path = "tasks." + task.getId();
            yaml.set(path + ".category", task.getCategory().name());
            yaml.set(path + ".description", task.getDescription());
            yaml.set(path + ".targetAmount", task.getTargetAmount());
            yaml.set(path + ".matchKeys", new ArrayList<>(task.getMatchKeys()));
            yaml.set(path + ".rewardType", task.getReward().type().name());
            yaml.set(path + ".rewardAmount", task.getReward().amount());
        }
        for (var playerEntry : progress.entrySet()) {
            for (var taskEntry : playerEntry.getValue().entrySet()) {
                var path = "progress." + playerEntry.getKey() + "." + taskEntry.getKey();
                yaml.set(path + ".count", taskEntry.getValue().getCount());
                yaml.set(path + ".completed", taskEntry.getValue().isCompleted());
            }
        }
        try {
            yaml.save(dataFile());
        } catch (IOException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Failed to save quest data", e);
        }
    }

    /**
     * Loads persisted quest state if the file exists and the saved tasks are still for the current quest day
     * (i.e. generated after the most recent {@link #RESET_ZONE} midnight). Returns false if there's nothing valid
     * to restore, in which case the caller should generate fresh tasks.
     */
    public boolean loadFromFile() {
        var file = dataFile();
        if (!file.exists()) {
            return false;
        }
        try {
            var yaml = YamlConfiguration.loadConfiguration(file);
            var lastGenStr = yaml.getString("lastGenerated");
            if (lastGenStr == null) {
                return false;
            }
            var savedLastGenerated = Instant.parse(lastGenStr);
            var periodStart = ZonedDateTime.now(RESET_ZONE).toLocalDate().atStartOfDay(RESET_ZONE).toInstant();
            if (savedLastGenerated.isBefore(periodStart)) {
                return false; // expired — saved data is from a previous day
            }

            var tasksSection = yaml.getConfigurationSection("tasks");
            if (tasksSection == null) {
                return false;
            }
            var loadedTasks = new ArrayList<Task>();
            for (String taskIdStr : tasksSection.getKeys(false)) {
                var section = tasksSection.getConfigurationSection(taskIdStr);
                if (section == null) {
                    continue;
                }
                loadedTasks.add(new Task(
                        UUID.fromString(taskIdStr),
                        TaskCategory.valueOf(section.getString("category")),
                        section.getString("description"),
                        section.getInt("targetAmount"),
                        new HashSet<>(section.getStringList("matchKeys")),
                        new TaskReward(RewardType.valueOf(section.getString("rewardType")),
                                section.getInt("rewardAmount"))
                ));
            }
            if (loadedTasks.isEmpty()) {
                return false;
            }

            var loadedProgress = new ConcurrentHashMap<UUID, Map<UUID, PlayerTaskProgress>>();
            var progressSection = yaml.getConfigurationSection("progress");
            if (progressSection != null) {
                for (String playerIdStr : progressSection.getKeys(false)) {
                    var playerSection = progressSection.getConfigurationSection(playerIdStr);
                    if (playerSection == null) {
                        continue;
                    }
                    var playerMap = new ConcurrentHashMap<UUID, PlayerTaskProgress>();
                    for (String taskIdStr : playerSection.getKeys(false)) {
                        var taskSection = playerSection.getConfigurationSection(taskIdStr);
                        if (taskSection == null) {
                            continue;
                        }
                        playerMap.put(UUID.fromString(taskIdStr), new PlayerTaskProgress(
                                taskSection.getInt("count"), taskSection.getBoolean("completed")));
                    }
                    loadedProgress.put(UUID.fromString(playerIdStr), playerMap);
                }
            }

            dailyTasks = List.copyOf(loadedTasks);
            lastGenerated = savedLastGenerated;
            progress.clear();
            progress.putAll(loadedProgress);
            return true;
        } catch (Exception e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Failed to load quest data, regenerating", e);
            return false;
        }
    }

    /**
     * Records progress for a player against every daily task in {@code category} whose target matches {@code key}.
     * Completing a task auto-grants its reward and notifies the player. Must be called on the main thread.
     *
     * @param key normalized subject (Material or EntityType {@code name()})
     */
    public void progress(Player player, TaskCategory category, String key, int amount) {
        if (amount <= 0) {
            return;
        }
        var playerProgress = progress.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        for (Task task : dailyTasks) {
            if (task.getCategory() != category || !task.matches(key)) {
                continue;
            }
            var taskProgress = playerProgress.computeIfAbsent(task.getId(), k -> new PlayerTaskProgress());
            if (taskProgress.increment(amount, task.getTargetAmount())) {
                completeTask(player, task);
            }
        }
    }

    private void completeTask(Player player, Task task) {
        task.getReward().grant(player);
        new SenderMessageHandler(player).sendMessage(
                "<green>Quest complete: <white>" + task.getDescription()
                        + " <gray>(+" + task.getReward().describe() + "<gray>)"
        );
        // save now rather than at the next 5-minute autosave: after a crash the task would be completable (and
        // paid out) again
        saveAsync();
        // bump the player's lifetime quest count and refresh their cached entry
        var playerId = player.getUniqueId();
        CompletableFuture
                .runAsync(() -> Tables.playerTable.incrementQuestsCompleted(playerId))
                .thenRun(() -> PlayerEntryCache.INSTANCE.remove(playerId));
    }

    /**
     * The player's progress on a given task, or {@code null} if they have not made any progress yet.
     */
    public PlayerTaskProgress getProgress(UUID playerId, UUID taskId) {
        var playerProgress = progress.get(playerId);
        return playerProgress == null ? null : playerProgress.get(taskId);
    }

    /**
     * How many of today's daily tasks the player has completed.
     */
    public int getCompletedToday(UUID playerId) {
        var playerProgress = progress.get(playerId);
        if (playerProgress == null) {
            return 0;
        }
        int completed = 0;
        for (Task task : dailyTasks) {
            var taskProgress = playerProgress.get(task.getId());
            if (taskProgress != null && taskProgress.isCompleted()) {
                completed++;
            }
        }
        return completed;
    }

    /**
     * How many of today's daily tasks the player still has left to complete.
     */
    public int getRemainingToday(UUID playerId) {
        return dailyTasks.size() - getCompletedToday(playerId);
    }

    /**
     * Schedules the next regeneration at the upcoming midnight in {@link #RESET_ZONE}, then re-schedules itself.
     * Recomputing each time keeps it correct across DST changes.
     */
    public void scheduleNextReset() {
        // round up (whole seconds truncated, so it could fire just before midnight and then again right after)
        long delayTicks = Math.max(1, (getTimeUntilReset().toMillis() + 49) / 50);
        Bukkit.getScheduler().runTaskLater(GardensPlayers.plugin, () -> {
            // a late tick or an early fire must not generate the same day's tasks twice
            if (lastGenerated.isBefore(startOfToday())) {
                generateDailyTasks();
                saveAsync();
            }
            scheduleNextReset();
        }, delayTicks);
    }

    private static Instant startOfToday() {
        return ZonedDateTime.now(RESET_ZONE).toLocalDate().atStartOfDay(RESET_ZONE).toInstant();
    }

    /**
     * Time left before today's tasks regenerate (until the next midnight in {@link #RESET_ZONE}).
     */
    public Duration getTimeUntilReset() {
        var now = ZonedDateTime.now(RESET_ZONE);
        var nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(RESET_ZONE);
        return Duration.between(now, nextMidnight);
    }

    /**
     * Time left before today's tasks regenerate, formatted for display (e.g. {@code "21h 4m"}).
     */
    public String getFormattedTimeUntilReset() {
        return formatDuration(getTimeUntilReset());
    }

    /**
     * Sends a friendly nudge to a player who still has daily tasks left, with the time remaining and a clickable
     * {@code /quests} link. Does nothing if they have finished everything for the day.
     */
    public void alertRemaining(Player player) {
        int remaining = getRemainingToday(player.getUniqueId());
        if (remaining <= 0) {
            return;
        }
        var mm = MiniMessage.miniMessage();
        var questWord = remaining == 1 ? "quest" : "quests";
        player.sendMessage(mm.deserialize(
                "<dark_gray><st>            </st> <gradient:#f6d365:#fda085><bold>✦ Daily Quests ✦</bold></gradient> <dark_gray><st>            </st>"));
        player.sendMessage(mm.deserialize(
                "  <gray>You have <yellow><b>" + remaining + "</b></yellow> " + questWord
                        + " <gray>left to complete today!"));
        player.sendMessage(mm.deserialize(
                "  <click:run_command:/quests><hover:show_text:'<green>Click to open your quests'>"
                        + "<green>▶ <green><u>/quests</u></green></click> "
                        + "<dark_gray>• <gray>resets in <white>" + getFormattedTimeUntilReset()));
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
}
