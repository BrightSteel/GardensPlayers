package com.gardensmc.gardensplayers.streak;

/**
 * How a streak moves: missed days use up savers or end it, and a counted day adds one. Pure, so it's tested on its own.
 */
public final class StreakRules {

    /** What catching a streak up to today did: savers spent on missed days, or the streak that ended (0 if none). */
    public record Settled(int saversUsed, int ended) {
        public boolean changed() {
            return saversUsed > 0 || ended > 0;
        }
    }

    private StreakRules() {
    }

    /**
     * Catches the streak up to {@code today}: each day missed since the last counted one (not counting today, which can
     * still be played) uses a saver; if there aren't enough, the streak ends and the savers go with it.
     */
    public static Settled settle(Streak streak, long today) {
        if (streak.current <= 0 || streak.lastDay < 0) {
            return new Settled(0, 0);
        }
        long missed = today - streak.lastDay - 1;
        if (missed <= 0) {
            return new Settled(0, 0);
        }
        if (missed <= streak.savers) {
            streak.savers -= (int) missed;
            // the missed days are covered, so yesterday counts as the last day in the run
            streak.lastDay = today - 1;
            return new Settled((int) missed, 0);
        }
        int ended = streak.current;
        streak.current = 0;
        streak.savers = 0;
        return new Settled(0, ended);
    }

    /**
     * Counts {@code today} (after {@link #settle}): the streak grows by one, or starts at 1. Every {@code saverEvery}
     * days in a row earns a saver, up to {@code maxSavers}. Returns the streak day it became, or 0 if today already
     * counted.
     */
    public static int count(Streak streak, long today, int saverEvery, int maxSavers) {
        if (streak.countedOn(today)) {
            return 0;
        }
        streak.current = streak.current > 0 && streak.lastDay == today - 1 ? streak.current + 1 : 1;
        streak.lastDay = today;
        streak.best = Math.max(streak.best, streak.current);
        if (saverEvery > 0 && streak.current % saverEvery == 0) {
            streak.savers = Math.min(maxSavers, streak.savers + 1);
        }
        return streak.current;
    }

    /** Whether counting this day earned a saver (it's a multiple of {@code saverEvery} and there was room). */
    public static boolean earnsSaver(int day, int saversBefore, int saverEvery, int maxSavers) {
        return saverEvery > 0 && day % saverEvery == 0 && saversBefore < maxSavers;
    }
}
