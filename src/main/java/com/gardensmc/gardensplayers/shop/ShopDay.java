package com.gardensmc.gardensplayers.shop;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The day shop limits count over: a real day, from midnight in America/Los_Angeles (as quests and the daily restart).
 * They used to count over a farming day, 20 real minutes, so they reset 72 times a day and barely limited anything.
 */
public final class ShopDay {

    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    /**
     * Keeps these day numbers clear of the farming-day numbers rows were stored under before, so an old row can never
     * read as today's.
     */
    private static final int OFFSET = 1_000_000;

    private ShopDay() {
    }

    public static int today() {
        return OFFSET + (int) LocalDate.now(ZONE).toEpochDay();
    }
}
