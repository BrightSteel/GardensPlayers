package com.gardensmc.gardensplayers.quest;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A blueprint for generating concrete {@link Task}s. Holds the category, a description format containing the
 * placeholders {@code {amount}} and {@code {target}}, a pool of possible {@link TaskTarget}s, and the amount range.
 * {@link #generate()} fills in random variables (target, amount, reward).
 */
@Getter
public class TaskTemplate {

    private final TaskCategory category;
    private final String descriptionFormat;
    private final List<TaskTarget> targets;
    private final int minAmount;
    private final int maxAmount;

    public TaskTemplate(TaskCategory category, String descriptionFormat, List<TaskTarget> targets,
                        int minAmount, int maxAmount) {
        this.category = category;
        this.descriptionFormat = descriptionFormat;
        this.targets = targets;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    /**
     * Generates a concrete task from this template with the given {@code reward}. The reward is supplied by the
     * caller so it can enforce collection-wide constraints (e.g. a daily gem budget).
     */
    public Task generate(TaskReward reward) {
        var random = ThreadLocalRandom.current();
        var target = targets.get(random.nextInt(targets.size()));
        var amount = random.nextInt(minAmount, maxAmount + 1);

        var description = descriptionFormat
                .replace("{amount}", String.valueOf(amount))
                .replace("{target}", target.displayName().toLowerCase());

        return new Task(category, description, amount, target.matchKeys(), reward);
    }
}
