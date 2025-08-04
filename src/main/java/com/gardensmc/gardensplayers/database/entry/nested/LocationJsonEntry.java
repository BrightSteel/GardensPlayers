package com.gardensmc.gardensplayers.database.entry.nested;

import com.google.gson.Gson;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

import static com.gardensmc.gardensplayers.util.MathUtil.round;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class LocationJsonEntry extends JsonEntry {
    private double x, y, z;
    private float yaw, pitch;
    private UUID world;

    public static LocationJsonEntry fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, LocationJsonEntry.class);
    }

    public static LocationJsonEntry fromLocation(Location location) {
        return new LocationJsonEntry()
                .setX(round(location.getX()))
                .setY(round(location.getY()))
                .setZ(round(location.getZ()))
                .setYaw(round(location.getYaw()))
                .setPitch(round(location.getPitch()))
                .setWorld(location.getWorld().getUID());
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}