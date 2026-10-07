package com.gardensmc.gardensplayers.resourceworld;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import static com.gardensmc.gardensplayers.resourceworld.ResourceWorlds.WORLDS;

/**
 * Pregenerated resource worlds kept in remote storage (Cloudflare R2) rather than on a server's disk, one zip per
 * world ({@code <prefix><name>.zip}, see {@link WorldArchive}).
 *
 * <ul>
 *     <li>The generating machine ({@code upload-generated}) uploads each world as it finishes, then deletes it.</li>
 *     <li>A server that resets ({@code keep-local}) downloads the oldest sets ahead, so a reset never waits on a
 *     download, and deletes a set from storage once a reset has used it.</li>
 * </ul>
 * All storage work runs one job at a time on its own thread.
 */
public final class ResourceWorldStorage {

    public static final ResourceWorldStorage INSTANCE = new ResourceWorldStorage();
    private static final String SUFFIX = ".zip";
    private static final String DOWNLOADING = ".downloading";

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "GardensPlayers resource world storage");
        thread.setDaemon(true);
        return thread;
    });
    // by live world: the names waiting in storage, oldest first (as of the last listing)
    private volatile Map<String, List<String>> remote = Map.of();
    private volatile Instant listedAt;
    private volatile String activity = "idle";
    // names queued for upload, so the same world isn't uploaded twice
    private final Set<String> uploading = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ResourceWorldStorage() {
    }

    public boolean enabled() {
        var config = GardensPlayers.config;
        return config.isStorageEnabled() && !config.getStorageEndpoint().isBlank() && !config.getStorageBucket().isBlank();
    }

    @Nullable
    private S3Store store() {
        if (!enabled()) return null;
        var c = GardensPlayers.config;
        return new S3Store(c.getStorageEndpoint(), c.getStorageBucket(), c.getStorageAccessKey(), c.getStorageSecretKey(), c.getStorageRegion());
    }

    private static String key(String name) {
        return GardensPlayers.config.getStoragePrefix() + name + SUFFIX;
    }

    // ---- what's in storage ----

    /** Full sets waiting in storage, as of the last listing. */
    public int remoteSets() {
        if (!enabled()) return 0;
        return WORLDS.keySet().stream().mapToInt(world -> remote.getOrDefault(world, List.of()).size()).min().orElse(0);
    }

    public List<String> remote(String world) {
        return remote.getOrDefault(world, List.of());
    }

    public String describe() {
        if (!enabled()) return "Remote storage is off.";
        return "In storage: " + remoteSets() + " sets" + (listedAt == null ? " (not listed yet)" : "") + ". Storage: " + activity + ".";
    }

    /** Lists the bucket again (on the storage thread). */
    public void refresh() {
        if (enabled()) worker.submit(this::list);
    }

    private void list() {
        var store = store();
        if (store == null) return;
        try {
            var names = new ArrayList<String>();
            for (var object : store.list(GardensPlayers.config.getStoragePrefix())) {
                var name = object.key().substring(GardensPlayers.config.getStoragePrefix().length());
                if (name.endsWith(SUFFIX)) names.add(name.substring(0, name.length() - SUFFIX.length()));
            }
            var byWorld = new LinkedHashMap<String, List<String>>();
            for (var world : WORLDS.keySet()) {
                byWorld.put(world, names.stream()
                        .filter(name -> ResourceWorlds.createdAt(name, world) != null)
                        // timestamped names sort by when they were made; old uuid names (no date) go first
                        .sorted(Comparator.comparingLong((String name) -> ResourceWorlds.createdAt(name, world).orElse(0)).thenComparing(name -> name))
                        .toList());
            }
            remote = byWorld;
            listedAt = Instant.now();
        } catch (IOException | RuntimeException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't list the resource world storage", e);
        }
    }

    // ---- the generating machine ----

    /** Uploads every finished pregenerated world on disk (then deletes it), if this machine uploads. */
    public void uploadFinished() {
        if (!enabled() || !GardensPlayers.config.isStorageUploadGenerated()) return;
        for (var world : WORLDS.keySet()) {
            for (var pregenerated : ResourceWorlds.available(world)) {
                if (!uploading.add(pregenerated.name())) continue;
                var folder = pregenerated.folder();
                var name = pregenerated.name();
                worker.submit(() -> upload(name, folder));
            }
        }
    }

    private void upload(String name, java.nio.file.Path folder) {
        var store = store();
        try {
            if (store == null || !Files.isDirectory(folder)) return;
            activity = "uploading " + name;
            long started = System.currentTimeMillis();
            var contents = new WorldArchive.Contents[1];
            store.upload(key(name), out -> contents[0] = WorldArchive.pack(folder, out));
            // only delete the local copy once storage lists it
            list();
            var world = WORLDS.keySet().stream().filter(w -> ResourceWorlds.createdAt(name, w) != null).findFirst().orElse("");
            if (!remote(world).contains(name)) throw new IOException("Uploaded " + name + " but storage doesn't list it");
            WorldArchive.deleteRecursively(folder);
            GardensPlayers.plugin.getLogger().info("Uploaded " + name + " (" + contents[0].files() + " files, "
                    + (contents[0].bytes() >> 20) + " MB) in " + (System.currentTimeMillis() - started) / 1000 + "s, and removed it here");
        } catch (IOException | RuntimeException e) {
            GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Uploading " + name + " failed; it stays on disk and will be tried again", e);
        } finally {
            uploading.remove(name);
            activity = "idle";
        }
    }

    // ---- the server that resets ----

    /** Downloads the oldest sets from storage until {@code keep-local} sets are ready on disk. */
    public void fillLocal() {
        if (!enabled() || GardensPlayers.config.getStorageKeepLocal() <= 0) return;
        worker.submit(this::fill);
    }

    private void fill() {
        var store = store();
        if (store == null) return;
        list();
        int wanted = GardensPlayers.config.getStorageKeepLocal();
        try {
            while (ResourceWorlds.resetsLeft() < wanted) {
                var local = new HashSet<String>();
                for (var world : WORLDS.keySet()) ResourceWorlds.available(world).forEach(p -> local.add(p.name()));
                // for each world short of the target, the oldest in storage not on disk yet; all found before any
                // download starts, so a set never comes down half
                var next = new ArrayList<String>();
                for (var world : WORLDS.keySet()) {
                    if (ResourceWorlds.available(world).size() >= wanted) continue;
                    var name = remote(world).stream().filter(n -> !local.contains(n)).findFirst();
                    if (name.isEmpty()) {
                        GardensPlayers.plugin.getLogger().warning("No " + world + " left in storage to download: run /genresource on the generating machine");
                        return;
                    }
                    next.add(name.get());
                }
                if (next.isEmpty()) return;
                for (var name : next) download(store, name);
            }
        } catch (IOException | RuntimeException e) {
            GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Downloading resource worlds from storage failed; it'll be tried again", e);
        } finally {
            activity = "idle";
        }
    }

    // unpacked beside the others under a temporary name, and renamed only once complete and checked
    private void download(S3Store store, String name) throws IOException {
        activity = "downloading " + name;
        long started = System.currentTimeMillis();
        var dimensions = ResourceWorlds.dimensions();
        var partial = dimensions.resolve(name + DOWNLOADING);
        WorldArchive.deleteRecursively(partial);
        WorldArchive.Contents contents;
        try (var in = store.download(key(name))) {
            contents = WorldArchive.unpack(in, partial);
        }
        Files.move(partial, dimensions.resolve(name));
        GardensPlayers.plugin.getLogger().info("Downloaded " + name + " (" + contents.files() + " files, "
                + (contents.bytes() >> 20) + " MB) in " + (System.currentTimeMillis() - started) / 1000 + "s");
    }

    /** After a reset: the worlds it used are gone from disk, so drop them from storage and download the next set. */
    public void afterReset(List<String> used) {
        if (!enabled()) return;
        worker.submit(() -> {
            var store = store();
            if (store == null) return;
            for (var name : used) {
                try {
                    store.delete(key(name));
                } catch (IOException e) {
                    GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't delete " + name + " from storage", e);
                }
            }
            list();
        });
        fillLocal();
    }

    /** Leftover half-downloaded folders (from a crash or restart mid-download), deleted at startup. */
    public void cleanUpPartialDownloads() {
        var dimensions = ResourceWorlds.dimensions();
        if (!Files.isDirectory(dimensions)) return;
        try (var paths = Files.list(dimensions)) {
            for (var path : paths.filter(p -> p.getFileName().toString().endsWith(DOWNLOADING)).toList()) {
                WorldArchive.deleteRecursively(path);
                GardensPlayers.plugin.getLogger().info("Removed an unfinished download: " + path.getFileName());
            }
        } catch (IOException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't clean up unfinished downloads", e);
        }
    }

    /** Runs {@code then} on the main thread once the storage thread has done what's queued before it. */
    public void afterQueued(Runnable then) {
        worker.submit(() -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, then));
    }
}
