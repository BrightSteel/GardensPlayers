package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import org.bukkit.entity.Player;

import java.util.List;

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
        return DatabaseConnector.INSTANCE.queryList(
                "SELECT warp.*, COUNT(warpVisits.warpId) AS visits FROM warp LEFT JOIN warpVisits ON warp.id = warpVisits.warpId GROUP BY id",
                WarpEntry::new
        );
    }

    public void updateWarpName(int warpId, String warpName) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE warp SET name = ? WHERE id = ? LIMIT 1",
                warpName,
                warpId
        );
    }

    public void updateWarpLocation(int warpId, LocationJsonEntry locationJsonEntry) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE warp SET location = ? WHERE id = ? LIMIT 1",
                locationJsonEntry.toJson(),
                warpId
        );
    }

    public void updateWarpMetadata(int warpId, String name, WarpEntry.Category category, boolean isPublic, String description) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE warp SET category = ?, name = ?, publicAccess = ?, description = ? WHERE id = ? LIMIT 1",
                category.toString(),
                name,
                isPublic,
                description,
                warpId
        );
    }

    public void createWarpEntry(Player player, String name, LocationJsonEntry locationJsonEntry, WarpEntry.Category category, boolean isPublic, String description) {
        DatabaseConnector.INSTANCE.update(
            "INSERT INTO warp (playerUUID, name, location, category, publicAccess, description) VALUES (?, ?, ?, ?, ?, ?)",
                player.getUniqueId().toString(),
                name,
                locationJsonEntry.toJson(),
                category.toString(),
                isPublic,
                description
        );
    }

    public void removeWarpEntry(int id) {
        DatabaseConnector.INSTANCE.update(
                "DELETE FROM warp WHERE id = ? LIMIT 1",
                id
        );
    }

}
