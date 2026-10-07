package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.TitleEntry;

import java.util.List;
import java.util.UUID;

/** The titles each player has earned, and which one they show. */
public class TitleTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS titles(
                    playerUUID varchar(36),
                    titleId varchar(40),
                    selected boolean NOT NULL DEFAULT false,
                    PRIMARY KEY (playerUUID, titleId),
                    FOREIGN KEY (playerUUID) REFERENCES player(uuid) ON DELETE CASCADE
                )
               """;
    }

    public List<TitleEntry> get(UUID player) {
        var list = DatabaseConnector.INSTANCE.queryList("SELECT * FROM titles WHERE playerUUID = ?", TitleEntry::new, player.toString());
        return list == null ? List.of() : list;
    }

    public void grant(UUID player, String titleId) {
        DatabaseConnector.INSTANCE.update("INSERT IGNORE INTO titles (playerUUID, titleId) VALUES (?, ?)", player.toString(), titleId);
    }

    /** Shows this title (or none, for null). */
    public void select(UUID player, String titleId) {
        DatabaseConnector.INSTANCE.update("UPDATE titles SET selected = (titleId = ?) WHERE playerUUID = ?", titleId == null ? "" : titleId, player.toString());
    }
}
