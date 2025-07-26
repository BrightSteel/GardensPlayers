package com.gardensmc.gardensplayers.database.entry;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Setter
@Accessors(chain = true)
public class LocationEntry extends MySQLEntry {

    private double x, y, z;
    private float yaw, pitch;
    private UUID world;

    @Override
    public LocationEntry populate(ResultSet resultSet) throws SQLException {
        this.x = resultSet.getDouble("x");
        this.y = resultSet.getDouble("y");
        this.z = resultSet.getDouble("z");
        this.yaw = resultSet.getFloat("yaw");
        this.pitch = resultSet.getFloat("pitch");
        this.world = UUID.fromString(resultSet.getString("world"));
        return this;
    }

    public static LocationEntry fromLocation(Location location) {
        return new LocationEntry()
                .setX(location.getX())
                .setY(location.getY())
                .setZ(location.getZ())
                .setYaw(location.getYaw())
                .setPitch(location.getPitch())
                .setWorld(location.getWorld().getUID());
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}
