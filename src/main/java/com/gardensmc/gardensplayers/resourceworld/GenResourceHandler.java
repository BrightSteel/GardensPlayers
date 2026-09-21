package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenResourceHandler {

    public static final GenResourceHandler INSTANCE = new GenResourceHandler();
    public static final Map<String, World.Environment> WORLDS = Map.of(
            "resource", World.Environment.NORMAL,
            "resource_nether", World.Environment.NETHER,
            "resource_the_end", World.Environment.THE_END
    );

    private GenResourceStatus status = GenResourceStatus.AVAILABLE;
    private final Queue<String> generationQueue = new PriorityQueue<>();
    private String generationInProgress;
    private Audience requestor;

    public boolean triggerGeneration(Audience requestor, int count) {
        if (status == GenResourceStatus.GENERATING) {
            return false;
        }
        this.requestor = requestor;
        status = GenResourceStatus.GENERATING;
//        var resourceWorlds = getLoadedResourceWorlds();

//        relocatePlayers(resourceWorlds);

//        resourceWorlds.forEach(world -> Bukkit.unloadWorld(world, false));
//        deleteResourceWorlds();

//        createResourceWorlds();
        for (int i = 0; i < count; i++){
            generationQueue.addAll(WORLDS.keySet()
                    .stream()
                    .map(name -> name + "." + UUID.randomUUID())
                    .toList()
            );
        }
        tick(); // start generation
        return true;
    }

    public void tick() {
        if (generationInProgress == null && generationQueue.isEmpty()) {
            return;
        }
        var chunkyAPI = GardensPlayers.chunkyAPI;
        if (generationInProgress == null) {
            generationInProgress = generationQueue.poll();
            String[] parts = generationInProgress.split("\\.");
            String worldKey = parts[0];
            var world = new WorldCreator(generationInProgress)
                    .environment(WORLDS.get(worldKey))
                    .createWorld();
            if (world != null) {
                var radius = GardensPlayers.config.getResourceWorldRadius() + 250;
                chunkyAPI.startTask(generationInProgress, "square", 0, 0, radius, radius, "concentric");
            } else {
                GardensPlayers.plugin.getLogger().severe("Failed to create resource world " + generationInProgress + " stopping generation");
                failGeneration();
            }
        } else if (!chunkyAPI.isRunning(generationInProgress)) {
            // may be generating lots of worlds, lets remove from memory and mv
            unloadResourceWorld(generationInProgress);
            generationInProgress = null;
            if (generationQueue.isEmpty()) {
                status = GenResourceStatus.AVAILABLE;
                requestor.sendMessage(MiniMessage.miniMessage().deserialize("Finished generating resource worlds!"));
            } else {
                tick(); // start next generation
            }
        }
    }

    private void unloadResourceWorld(String name) {
       if (!Bukkit.unloadWorld(name, false)) {
           GardensPlayers.plugin.getLogger().severe("Failed to remove: " + name);
       }
    }

//    private void createResourceWorlds() {
//        WORLDS.forEach((s, env) -> new WorldCreator(s).environment(env).createWorld());
//    }

    private void failGeneration() {
        status = GenResourceStatus.FAILED;
        generationInProgress = null;
        generationQueue.clear();
    }

//    private void deleteResourceWorlds() {
//        var worldDirectory = Bukkit.getServer().getWorldContainer().toPath();
//        try (var stream = Files.walk(worldDirectory)) {
//            stream.filter(Files::isDirectory)
//                    .filter(path -> path.getFileName().toString().contains("resource"))
//                    .forEach(this::deleteDirectory);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to delete resource worlds", e);
//        }
//    }

    private void deleteDirectory(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete directory files", e);
                        }
                    });
        } catch (IOException ex) {
            throw new RuntimeException("Failed to delete directory: " + dir, ex);
        }
    }

    private List<World> getLoadedResourceWorlds() {
        return Bukkit.getWorlds()
                .stream()
                .filter(world -> world.getName().toLowerCase().contains("resource"))
                .toList();
    }
}
