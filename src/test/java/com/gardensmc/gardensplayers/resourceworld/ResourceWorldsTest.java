package com.gardensmc.gardensplayers.resourceworld;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceWorldsTest {

    @Test
    void namesSortByWhenTheyWereMade() {
        var march = ResourceWorlds.newName("resource_world", LocalDateTime.of(2026, 3, 1, 9, 5, 0));
        var october = ResourceWorlds.newName("resource_world", LocalDateTime.of(2026, 10, 7, 14, 30, 59));
        assertEquals("resource_world.20260301-090500", march);
        assertTrue(march.compareTo(october) < 0);
        assertTrue(ResourceWorlds.createdAt(march, "resource_world").getAsLong()
                < ResourceWorlds.createdAt(october, "resource_world").getAsLong());
    }

    @Test
    void oldUuidNamesStillCount() {
        var created = ResourceWorlds.createdAt("resource_nether.0898868e-55da-4074-b2a3-e80cafd94af2", "resource_nether");
        assertNotNull(created);
        assertTrue(created.isEmpty(), "dated by the folder");
        assertNotNull(ResourceWorlds.createdAt("resource.af2034fb-a23b-44c6-a5e7-ce0cea94f066", "resource_world"), "the overworld's old name");
    }

    @Test
    void otherFoldersDoNot() {
        assertNull(ResourceWorlds.createdAt("resource_world", "resource_world"), "the live world itself");
        assertNull(ResourceWorlds.createdAt("resource_nether.20260301-090500", "resource_world"), "another world's");
        assertNull(ResourceWorlds.createdAt("resource.af2034fb-a23b-44c6-a5e7-ce0cea94f066", "resource_nether"));
        assertNull(ResourceWorlds.createdAt("overworld", "resource_world"));
        assertNull(ResourceWorlds.createdAt("resource_world.20261399-000000", "resource_world"), "not a real date");
    }
}
