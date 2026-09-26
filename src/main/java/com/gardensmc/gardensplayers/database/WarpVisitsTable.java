package com.gardensmc.gardensplayers.database;

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
        DatabaseConnector.INSTANCE.update(
            "INSERT IGNORE INTO warpVisits (warpId, playerUUID) VALUES (?, ?)",
                warpId,
                playerUUID.toString()
        );
    }

}
