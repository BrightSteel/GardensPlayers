package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.util.Worlds;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenResourceHandler {

    public static final GenResourceHandler INSTANCE = new GenResourceHandler();
    public static final Map<String, World.Environment> WORLDS = Map.of(
            Worlds.RESOURCE, World.Environment.NORMAL, // the production name
            Worlds.RESOURCE_NETHER, World.Environment.NETHER,
            Worlds.RESOURCE_END, World.Environment.THE_END
    );

    private GenResourceStatus status = GenResourceStatus.AVAILABLE;
    private final Queue<String> generationQueue = new PriorityQueue<>();
    private String generationInProgress;
    private Audience requestor;

    public boolean triggerGeneration(Audience requestor, int count, World.Environment environment) {
        if (status == GenResourceStatus.GENERATING) {
            return false;
        }
        this.requestor = requestor;
        status = GenResourceStatus.GENERATING;

        for (int i = 0; i < count; i++){
            var worlds = environment != null
                    ? WORLDS.entrySet().stream().filter(e -> e.getValue() == environment).toList()
                    : WORLDS.entrySet().stream().toList();
            generationQueue.addAll(worlds
                    .stream()
                    .map(world -> world.getKey() + "." + UUID.randomUUID())
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

    private void failGeneration() {
        status = GenResourceStatus.FAILED;
        generationInProgress = null;
        generationQueue.clear();
    }

}
