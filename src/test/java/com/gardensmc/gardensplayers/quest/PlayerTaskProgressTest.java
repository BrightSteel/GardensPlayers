package com.gardensmc.gardensplayers.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTaskProgressTest {

    @Test
    void completesOnceAtTheTarget() {
        var progress = new PlayerTaskProgress();
        assertFalse(progress.increment(3, 5));
        assertTrue(progress.increment(3, 5));
        assertEquals(5, progress.getCount(), "clamped to the target");
        assertFalse(progress.increment(1, 5), "a completed task can't complete again");
    }

    // placing and breaking the same block nets zero
    @Test
    void decrementTakesProgressBackButNotBelowZero() {
        var progress = new PlayerTaskProgress();
        progress.increment(2, 10);
        progress.decrement(1);
        assertEquals(1, progress.getCount());
        progress.decrement(5);
        assertEquals(0, progress.getCount());
    }

    @Test
    void completedTaskKeepsItsProgress() {
        var progress = new PlayerTaskProgress();
        progress.increment(5, 5);
        progress.decrement(1);
        assertEquals(5, progress.getCount());
        assertTrue(progress.isCompleted());
    }
}
