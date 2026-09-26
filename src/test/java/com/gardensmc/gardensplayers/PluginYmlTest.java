package com.gardensmc.gardensplayers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

// catches plugin.yml mistakes that would otherwise only show up as "plugin failed to load" on the server
class PluginYmlTest {

    private final YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new InputStreamReader(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("plugin.yml")), StandardCharsets.UTF_8));

    @Test
    void mainClassExists() {
        var main = pluginYml.getString("main");
        assertNotNull(main);
        // look for the compiled class rather than loading it, which would need the other plugins on the classpath
        assertNotNull(getClass().getClassLoader().getResource(main.replace('.', '/') + ".class"), "no class " + main);
    }

    // plugins the code calls unconditionally must be hard dependencies, or it fails to enable without them
    @Test
    void requiredPluginsAreHardDependencies() {
        assertTrue(pluginYml.getStringList("depend").containsAll(List.of(
                "Vault", "GardensFurniture", "GardensCrops", "WorldGuard", "floodgate", "packetevents")));
    }

    @Test
    void optionalPluginsAreSoftDependencies() {
        assertTrue(pluginYml.getStringList("softdepend").containsAll(List.of("GriefPrevention", "PlaceholderAPI")));
    }
}
