package com.gardensmc.gardensplayers.resourceworld;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldArchiveTest {

    @TempDir
    Path temp;

    private Path world() throws IOException {
        var world = temp.resolve("resource_world.20261007-150045");
        Files.createDirectories(world.resolve("region"));
        Files.createDirectories(world.resolve("data"));
        var region = new byte[300_000];
        for (int i = 0; i < region.length; i++) region[i] = (byte) (i * 31);
        Files.write(world.resolve("region/r.0.0.mca"), region);
        Files.writeString(world.resolve("paper-world.yml"), "_version: 31\n");
        Files.writeString(world.resolve("data/raids.dat"), "x");
        Files.writeString(world.resolve(ResourceWorlds.UNFINISHED), "never packed");
        return world;
    }

    @Test
    void roundTrip() throws IOException {
        var world = world();
        var zipped = new ByteArrayOutputStream();
        var packed = WorldArchive.pack(world, zipped);
        assertEquals(3, packed.files(), "the marker stays behind");

        var copy = temp.resolve("copy");
        var unpacked = WorldArchive.unpack(new ByteArrayInputStream(zipped.toByteArray()), copy);
        assertEquals(packed, unpacked);
        assertArrayEquals(Files.readAllBytes(world.resolve("region/r.0.0.mca")), Files.readAllBytes(copy.resolve("region/r.0.0.mca")));
        assertFalse(Files.exists(copy.resolve(ResourceWorlds.UNFINISHED)));
    }

    @Test
    void aCutShortArchiveIsRefusedAndCleanedUp() throws IOException {
        var zipped = new ByteArrayOutputStream();
        WorldArchive.pack(world(), zipped);
        var truncated = Arrays.copyOf(zipped.toByteArray(), zipped.size() / 2);
        var copy = temp.resolve("copy");
        assertThrows(IOException.class, () -> WorldArchive.unpack(new ByteArrayInputStream(truncated), copy));
        assertFalse(Files.exists(copy), "nothing half-written is left");
    }
}
