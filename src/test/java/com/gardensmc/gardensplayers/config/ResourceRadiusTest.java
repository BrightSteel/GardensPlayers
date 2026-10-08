package com.gardensmc.gardensplayers.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceRadiusTest {

    private static Config defaults() {
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(ResourceRadiusTest.class.getResourceAsStream("/config.yml"))));
        return new Config(yaml);
    }

    @Test
    void eachWorldHasItsOwnRadius() {
        var config = defaults();
        assertEquals(6000, config.resourceRadius("resource_world"));
        assertEquals(4000, config.resourceRadius("resource_nether"));
        assertEquals(4000, config.resourceRadius("resource_the_end"));
    }

    @Test
    void pregeneratedCopiesUseTheirWorldsRadius() {
        var config = defaults();
        assertEquals(6000, config.resourceRadius("resource_world.20261008-090000"));
        assertEquals(4000, config.resourceRadius("resource_nether.20261008-090000"));
        assertEquals(6000, config.resourceRadius("resource.af2034fb-a23b-44c6-a5e7-ce0cea94f066"));
    }
}
