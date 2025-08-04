package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class WarpTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS warp(
                    id int NOT NULL AUTO_INCREMENT,
                    playerUUID varchar(36) NOT NULL,
                    name varchar(100) NOT NULL,
                    location json NOT NULL,
                    category varchar(36) NOT NULL,
                    publicAccess boolean NOT NULL DEFAULT true,
                    serverWarp boolean NOT NULL DEFAULT false,
                    description varchar(200),
                    PRIMARY KEY (id)
                )
               """;
    }

    public List<WarpEntry> getWarpEntries() {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    "SELECT warp.*, COUNT(warpVisits.warpId) AS visits FROM warp LEFT JOIN warpVisits ON warp.id = warpVisits.warpId GROUP BY id",
                    WarpEntry::new
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public WarpEntry getWarpEntry(int id) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    "SELECT * FROM warp LEFT JOIN (SELECT COUNT(warpVisits.warpId) FROM warpVisits) visits ON visits.warpId = warp.id WHERE warp.id = ?",
                    WarpEntry::new,
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateWarpName(int warpId, String warpName) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE warp SET name = ? WHERE id = ? LIMIT 1",
                    warpName,
                    warpId
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateWarpLocation(int warpId, LocationJsonEntry locationJsonEntry) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE warp SET location = ? WHERE id = ? LIMIT 1",
                    locationJsonEntry.toJson(),
                    warpId
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateWarpMetadata(int warpId, String name, WarpEntry.Category category, boolean isPublic, String description) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE warp SET category = ?, name = ?, publicAccess = ?, description = ? WHERE id = ? LIMIT 1",
                    category.toString(),
                    name,
                    isPublic,
                    description,
                    warpId
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createWarpEntry(Player player, String name, LocationJsonEntry locationJsonEntry, WarpEntry.Category category, boolean isPublic, String description) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                "INSERT INTO warp (playerUUID, name, location, category, publicAccess, description) VALUES (?, ?, ?, ?, ?, ?)",
                    player.getUniqueId().toString(),
                    name,
                    locationJsonEntry.toJson(),
                    category.toString(),
                    isPublic,
                    description
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeWarpEntry(int id) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM warp WHERE id = ? LIMIT 1",
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
