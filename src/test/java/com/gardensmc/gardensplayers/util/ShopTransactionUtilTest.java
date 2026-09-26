package com.gardensmc.gardensplayers.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopTransactionUtilTest {

    @Test
    void noLimitSellsEverythingRequested() {
        assertEquals(128, ShopTransactionUtil.sellAmount(128, null));
    }

    @Test
    void clampsToWhatIsLeftOfTheLimit() {
        assertEquals(10, ShopTransactionUtil.sellAmount(64, 10));
        assertEquals(64, ShopTransactionUtil.sellAmount(64, 100));
    }

    @Test
    void metLimitSellsNothing() {
        assertEquals(0, ShopTransactionUtil.sellAmount(64, 0));
        assertEquals(0, ShopTransactionUtil.sellAmount(64, -5));
    }
}
