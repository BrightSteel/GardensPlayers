package com.gardensmc.gardensplayers.resourceworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A world folder as one zip, for keeping in storage. The last entry is a manifest of how many files and bytes went in,
 * so unpacking can tell a whole archive from one that was cut short.
 */
public final class WorldArchive {

    static final String MANIFEST = "gardens-resource-world.properties";

    /** What a world folder holds: its files and bytes (outside the manifest). */
    public record Contents(long files, long bytes) {
    }

    private WorldArchive() {
    }

    /** Zips a world folder's contents (paths relative to it) to {@code out}, ending with the manifest. */
    public static Contents pack(Path folder, OutputStream out) throws IOException {
        long files = 0, bytes = 0;
        var zip = new ZipOutputStream(out);
        zip.setLevel(Deflater.DEFAULT_COMPRESSION);
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(folder)) {
            paths = walk.filter(Files::isRegularFile).sorted().toList();
        }
        for (var path : paths) {
            var name = folder.relativize(path).toString().replace('\\', '/');
            // a half-made marker never travels (the uploader only takes finished worlds, but just in case)
            if (name.equals(ResourceWorlds.UNFINISHED) || name.equals("session.lock")) continue;
            zip.putNextEntry(new ZipEntry(name));
            bytes += Files.copy(path, zip);
            zip.closeEntry();
            files++;
        }
        zip.putNextEntry(new ZipEntry(MANIFEST));
        zip.write(("files=" + files + "\nbytes=" + bytes + "\n").getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.finish();
        zip.flush();
        return new Contents(files, bytes);
    }

    /**
     * Unzips into {@code target} (which mustn't exist yet), checking everything against the manifest. On any problem
     * the half-written folder is deleted and the error thrown.
     */
    public static Contents unpack(InputStream in, Path target) throws IOException {
        if (Files.exists(target)) throw new IOException(target + " already exists");
        Files.createDirectories(target);
        long files = 0, bytes = 0;
        Properties manifest = null;
        try (var zip = new ZipInputStream(in)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals(MANIFEST)) {
                    manifest = new Properties();
                    manifest.load(new StringReader(new String(zip.readAllBytes(), StandardCharsets.UTF_8)));
                    continue;
                }
                var path = target.resolve(entry.getName()).normalize();
                // never write outside the folder, whatever the archive says
                if (!path.startsWith(target)) throw new IOException("Archive entry outside the world folder: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                    continue;
                }
                Files.createDirectories(path.getParent());
                bytes += Files.copy(zip, path);
                files++;
            }
            if (manifest == null) throw new IOException("The archive has no manifest: it was cut short");
            var expected = new Contents(Long.parseLong(manifest.getProperty("files", "-1")), Long.parseLong(manifest.getProperty("bytes", "-1")));
            var got = new Contents(files, bytes);
            if (!expected.equals(got)) throw new IOException("The archive held " + got + " but its manifest says " + expected);
            return got;
        } catch (IOException | RuntimeException e) {
            deleteRecursively(target);
            throw e;
        }
    }

    static void deleteRecursively(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (Stream<Path> walk = Files.walk(folder)) {
            for (var path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
