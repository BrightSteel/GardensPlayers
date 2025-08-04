package com.gardensmc.gardensplayers.database;

import java.sql.SQLException;
import java.util.UUID;

public class WarpVisitsTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS warpVisits(
                    warpId int NOT NULL,
                    playerUUID varchar(36),
                    PRIMARY KEY (warpId, playerUUID),
                    FOREIGN KEY (warpId) REFERENCES warp(id) ON DELETE CASCADE
                )
               """;
    }

    public void createWarpVisitIfNotExists(int warpId, UUID playerUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                "INSERT IGNORE INTO warpVisits (warpId, playerUUID) VALUES (?, ?)",
                    warpId,
                    playerUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
