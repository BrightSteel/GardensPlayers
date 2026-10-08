package com.gardensmc.gardensplayers.resourceworld;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerationFillTest {

    private static Map<String, Integer> have(int world, int nether, int end) {
        var have = new LinkedHashMap<String, Integer>();
        have.put("resource_world", world);
        have.put("resource_nether", nether);
        have.put("resource_the_end", end);
        return have;
    }

    @Test
    void evenSetsMakeOneOfEach() {
        assertEquals(have(2, 2, 2), GenResourceHandler.toMake(have(4, 4, 4), 2));
    }

    @Test
    void aRunStoppedAfterTheEndTopsUpTheOthers() {
        assertEquals(have(1, 1, 0), GenResourceHandler.toMake(have(4, 4, 5), 1));
    }

    @Test
    void worldsAheadAreCountedTowardLaterSets() {
        assertEquals(have(3, 2, 1), GenResourceHandler.toMake(have(0, 1, 2), 3));
    }

    @Test
    void nothingStoredMakesFullSets() {
        assertEquals(have(1, 1, 1), GenResourceHandler.toMake(have(0, 0, 0), 1));
    }
}
