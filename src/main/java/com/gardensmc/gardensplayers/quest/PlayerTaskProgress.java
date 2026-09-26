package com.gardensmc.gardensplayers.quest;

import lombok.Getter;

/**
 * Mutable per-player progress on a single daily {@link Task}.
 */
@Getter
public class PlayerTaskProgress {

    private int count;
    private boolean completed;

    public PlayerTaskProgress() {
    }

    /**
     * Reconstructs progress from persisted data.
     */
    public PlayerTaskProgress(int count, boolean completed) {
        this.count = count;
        this.completed = completed;
    }

    /**
     * Adds progress, clamping at {@code targetAmount}.
     *
     * @return {@code true} if this increment caused the task to transition to completed
     */
    public boolean increment(int amount, int targetAmount) {
        if (completed) {
            return false;
        }
        count = Math.min(count + amount, targetAmount);
        if (count >= targetAmount) {
            completed = true;
            return true;
        }
        return false;
    }

    // takes progress back (never below 0); a completed task stays completed, its reward is already paid
    public void decrement(int amount) {
        if (!completed) {
            count = Math.max(0, count - amount);
        }
    }
}
