package com.gardensmc.gardensplayers.streak;

/**
 * A player's login streak: days in a row they've finished enough daily quest tasks, their best ever, the last day that
 * counted (an epoch day in America/Los_Angeles, or -1 for never) and the streak savers they hold.
 */
public final class Streak {

    int current;
    int best;
    long lastDay = -1;
    int savers;

    public Streak() {
    }

    public Streak(int current, int best, long lastDay, int savers) {
        this.current = current;
        this.best = best;
        this.lastDay = lastDay;
        this.savers = savers;
    }

    public int current() {
        return current;
    }

    public int best() {
        return best;
    }

    public long lastDay() {
        return lastDay;
    }

    public int savers() {
        return savers;
    }

    public boolean countedOn(long day) {
        return current > 0 && lastDay == day;
    }

    public Streak copy() {
        return new Streak(current, best, lastDay, savers);
    }
}
