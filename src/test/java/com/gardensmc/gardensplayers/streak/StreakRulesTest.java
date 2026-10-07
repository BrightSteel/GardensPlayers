package com.gardensmc.gardensplayers.streak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreakRulesTest {

    @Test
    void daysInARowGrow() {
        var streak = new Streak();
        assertEquals(1, StreakRules.count(streak, 100, 7, 2));
        assertEquals(2, StreakRules.count(streak, 101, 7, 2));
        assertEquals(0, StreakRules.count(streak, 101, 7, 2), "a day only counts once");
        assertEquals(2, streak.best());
    }

    @Test
    void everySeventhDayEarnsASaverUpToTwo() {
        var streak = new Streak();
        for (long day = 1; day <= 21; day++) StreakRules.count(streak, day, 7, 2);
        assertEquals(21, streak.current());
        assertEquals(2, streak.savers());
    }

    @Test
    void saversCoverMissedDays() {
        var streak = new Streak(9, 9, 100, 2);
        // played day 100, missed 101 and 102, back on 103
        var settled = StreakRules.settle(streak, 103);
        assertEquals(2, settled.saversUsed());
        assertEquals(0, streak.savers());
        assertEquals(10, StreakRules.count(streak, 103, 7, 2));
    }

    @Test
    void notEnoughSaversEndsTheStreak() {
        var streak = new Streak(9, 12, 100, 1);
        var settled = StreakRules.settle(streak, 103);
        assertEquals(9, settled.ended());
        assertEquals(0, streak.current());
        assertEquals(0, streak.savers());
        assertEquals(12, streak.best(), "the best streak is kept");
        assertEquals(1, StreakRules.count(streak, 103, 7, 2));
    }

    @Test
    void todayAndYesterdayChangeNothing() {
        var streak = new Streak(4, 4, 100, 1);
        assertTrue(!StreakRules.settle(streak, 100).changed());
        assertTrue(!StreakRules.settle(streak, 101).changed());
        assertEquals(1, streak.savers());
    }
}
