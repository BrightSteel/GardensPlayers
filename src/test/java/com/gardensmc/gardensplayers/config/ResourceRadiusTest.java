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
    void aServerConfigWithItsOwnResourceWorldsSectionStillGetsTheDefaults() {
        // like dev's: resourceWorlds.reset and .storage set, no .radius; the jar's config.yml is the defaults
        var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(ResourceRadiusTest.class.getResourceAsStream("/config.yml"))));
        var server = new YamlConfiguration();
        server.setDefaults(defaults);
        server.set("resourceWorlds.reset.enabled", false);
        server.set("resourceWorlds.storage.upload-generated", true);
        var config = new Config(server);
        assertEquals(6000, config.resourceRadius("resource_world"));
        assertEquals(4000, config.resourceRadius("resource_the_end.20261008-090000"));
    }

    @Test
    void pregeneratedCopiesUseTheirWorldsRadius() {
        var config = defaults();
        assertEquals(6000, config.resourceRadius("resource_world.20261008-090000"));
        assertEquals(4000, config.resourceRadius("resource_nether.20261008-090000"));
        assertEquals(6000, config.resourceRadius("resource.af2034fb-a23b-44c6-a5e7-ce0cea94f066"));
    }
}
