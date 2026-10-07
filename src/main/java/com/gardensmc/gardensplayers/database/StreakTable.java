package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.StreakEntry;
import com.gardensmc.gardensplayers.streak.Streak;

import java.util.UUID;

/** Each player's login streak (see streak/StreakHandler). */
public class StreakTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS streaks(
                    playerUUID varchar(36) PRIMARY KEY,
                    current int NOT NULL DEFAULT 0,
                    best int NOT NULL DEFAULT 0,
                    lastDay bigint NOT NULL DEFAULT -1,
                    savers int NOT NULL DEFAULT 0,
                    FOREIGN KEY (playerUUID) REFERENCES player(uuid) ON DELETE CASCADE
                )
               """;
    }

    public Streak get(UUID player) {
        var entry = DatabaseConnector.INSTANCE.queryOne("SELECT * FROM streaks WHERE playerUUID = ?", StreakEntry::new, player.toString());
        return entry == null ? new Streak() : entry.toStreak();
    }

    public void save(UUID player, Streak streak) {
        DatabaseConnector.INSTANCE.update(
                """
                INSERT INTO streaks (playerUUID, current, best, lastDay, savers) VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE current = VALUES(current), best = VALUES(best), lastDay = VALUES(lastDay), savers = VALUES(savers)
                """,
                player.toString(), streak.current(), streak.best(), streak.lastDay(), streak.savers()
        );
    }
}
