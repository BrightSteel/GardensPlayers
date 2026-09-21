package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.gardensmc.gardensplayers.resourceworld.GenResourceHandler.WORLDS;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResetResourceHandler {

    public static final ResetResourceHandler INSTANCE = new ResetResourceHandler();

    public void triggerReset() {
        Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize("Resource worlds are resetting, they will be temporarily unavailable"));
        relocatePlayers(WORLDS.keySet()
                .stream()
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull)
                .toList());
        deleteResourceWorlds();
        prepResourceWorldsForImport();
        importResourceWorlds();
        renderMap();
        Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize("Resource worlds have been reset."));
    }

    private void renderMap() {
        WORLDS.keySet().forEach(worldName -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "squaremap resetmap " + worldName);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "squaremap fullrender " + worldName);
        });
    }

    private void importResourceWorlds() {
        WORLDS.forEach((key, value) -> {
            var world = new WorldCreator(key).environment(value).createWorld();
            if (world == null) {
                GardensPlayers.plugin.getLogger().severe("Failed to import resource world: " + key + ".");
            }
        });
    }

    private void prepResourceWorldsForImport() {
        var worldDirectory = Bukkit.getServer().getWorldContainer().toPath();
        WORLDS.keySet().forEach(name -> {
            var randomWorldPath = getRandomWorldName(worldDirectory, name);
            try {
                Files.move(randomWorldPath, randomWorldPath.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to prep resource world for import: " + name, e);
            }
        });
    }

    private Path getRandomWorldName(Path dir, String prefix) {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .findAny()
                    .orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteResourceWorlds() {
        for (String name : WORLDS.keySet()) {
            var world = Bukkit.getWorld(name);
            if (world != null) {
                if (!Bukkit.unloadWorld(world, false)) {
                    GardensPlayers.plugin.getLogger().severe("Failed to unload resource world for deletion: " + name);
                    return;
                }
            }
            var worldDir = new File(Bukkit.getWorldContainer(), name);
            if (worldDir.exists()) {
                try {
                    FileUtils.deleteDirectory(worldDir);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete world directory", e);
                }
            }
        }
    }

    private void relocatePlayers(List<World> worlds) {
        worlds.stream()
                .flatMap(world -> world.getPlayers().stream())
                .forEach(player -> {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("Resource World is regenerating, moving you to spawn..."));
                    player.chat("/spawn");
                });
    }
}
