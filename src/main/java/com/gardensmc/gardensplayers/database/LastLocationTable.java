package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.LocationEntry;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class LastLocationTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
               CREATE TABLE IF NOT EXISTS lastLocation(
                    playerUUID varchar(36),
                    x double NOT NULL,
                    y double NOT NULL,
                    z double NOT NULL,
                    yaw float NOT NULL,
                    pitch float NOT NULL,
                    world varchar(36) NOT NULL,
                    PRIMARY KEY (playerUUID),
                    FOREIGN KEY (playerUUID) REFERENCES player(uuid) ON DELETE CASCADE
               )
               """;
    }

    public LocationEntry createLastLocationEntry(Player player) {
        try {
            var location = player.getLocation();
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO lastLocation (playerUUID, x, y, z, yaw, pitch, world) VALUES (?, round(?, 2), round(?, 2), round(?, 2), round(?, 2), round(?, 2), ?)",
                    player.getUniqueId().toString(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    location.getWorld().getUID().toString()
            );
            return LocationEntry.fromLocation(location);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public LocationEntry setLastLocationEntry(Player player) {
        try {
            var location = player.getLocation();
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE lastLocation SET x = round(?, 2), y = round(?, 2), z = round(?, 2), yaw = round(?, 2), pitch = round(?, 2), world = ? WHERE playerUUID = ?",
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    location.getWorld().getUID().toString(),
                    player.getUniqueId().toString()
            );
            return LocationEntry.fromLocation(location);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
