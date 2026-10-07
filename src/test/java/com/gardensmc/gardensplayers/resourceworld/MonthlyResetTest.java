package com.gardensmc.gardensplayers.resourceworld;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyResetTest {

    @Test
    void dueOnTheDayOfANewMonth() {
        assertTrue(ResetResourceHandler.isDue(LocalDate.of(2026, 11, 1), YearMonth.of(2026, 10), 1));
    }

    @Test
    void notTwiceInAMonth() {
        assertFalse(ResetResourceHandler.isDue(LocalDate.of(2026, 11, 1), YearMonth.of(2026, 11), 1));
        assertFalse(ResetResourceHandler.isDue(LocalDate.of(2026, 11, 30), YearMonth.of(2026, 11), 1));
    }

    @Test
    void notBeforeTheDay() {
        assertFalse(ResetResourceHandler.isDue(LocalDate.of(2026, 11, 2), YearMonth.of(2026, 10), 3));
        assertTrue(ResetResourceHandler.isDue(LocalDate.of(2026, 11, 3), YearMonth.of(2026, 10), 3));
    }

    @Test
    void aMissedMonthCatchesUpOnce() {
        // the server was down all of November: the first check in December resets once
        assertTrue(ResetResourceHandler.isDue(LocalDate.of(2026, 12, 9), YearMonth.of(2026, 10), 1));
    }

    @Test
    void acrossTheYear() {
        assertTrue(ResetResourceHandler.isDue(LocalDate.of(2027, 1, 1), YearMonth.of(2026, 12), 1));
    }
}
