package com.gardensmc.gardensplayers.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// fractional Vault amounts must never create money: withdrawals round up, deposits round down
class GardensEconomyProviderTest {

    @Test
    void withdrawalsRoundUp() {
        assertEquals(1, GardensEconomyProvider.withdrawalAmount(0.01));
        assertEquals(10, GardensEconomyProvider.withdrawalAmount(9.5));
        assertEquals(10, GardensEconomyProvider.withdrawalAmount(10));
    }

    @Test
    void depositsRoundDown() {
        assertEquals(0, GardensEconomyProvider.depositAmount(0.99));
        assertEquals(9, GardensEconomyProvider.depositAmount(9.5));
        assertEquals(10, GardensEconomyProvider.depositAmount(10));
    }
}
