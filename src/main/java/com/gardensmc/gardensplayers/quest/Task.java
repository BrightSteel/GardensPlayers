package com.gardensmc.gardensplayers.quest;

import lombok.Getter;

import java.util.Set;
import java.util.UUID;

/**
 * A concrete daily task generated from a {@link TaskTemplate}. Immutable and shared by all players for the day;
 * per-player progress is tracked separately in {@code QuestHandler}.
 */
@Getter
public class Task {

    private final UUID id;
    private final TaskCategory category;
    private final String description;
    private final int targetAmount;
    private final Set<String> matchKeys;
    private final TaskReward reward;

    public Task(TaskCategory category, String description, int targetAmount, Set<String> matchKeys,
                TaskReward reward) {
        this(UUID.randomUUID(), category, description, targetAmount, matchKeys, reward);
    }

    /**
     * Reconstructs a task with a known id (used when loading persisted quest data).
     */
    public Task(UUID id, TaskCategory category, String description, int targetAmount, Set<String> matchKeys,
                TaskReward reward) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.targetAmount = targetAmount;
        this.matchKeys = matchKeys;
        this.reward = reward;
    }

    /**
     * Whether an event subject (a Material or EntityType name) counts towards this task. An empty match set means
     * the task accepts any subject the category's listener forwards.
     */
    public boolean matches(String key) {
        return matchKeys.isEmpty() || matchKeys.contains(key);
    }
}
