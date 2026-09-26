package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.RewardEntry;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public class RewardsTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS rewards(
                    id varchar(40),
                    playerUUID varchar(36),
                    reward text,
                    claimed boolean NOT NULL DEFAULT false,
                    PRIMARY KEY (id, playerUUID),
                    FOREIGN KEY (playerUUID) REFERENCES player(uuid) ON DELETE CASCADE
                )
               """;
    }

    @Nullable
    public List<RewardEntry> getUnclaimedRewards(String playerUUID) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    "SELECT * FROM rewards where playerUUID = ? AND claimed = false",
                    RewardEntry::new,
                    playerUUID
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasReward(String playerUUID, String rewardId) {
        try {
            var rewardEntry = DatabaseConnector.INSTANCE.querySingleton(
                    "SELECT * FROM rewards where playerUUID = ? AND id = ? LIMIT 1",
                    RewardEntry::new,
                    playerUUID,
                    rewardId
            );
            return rewardEntry != null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void giveReward(String playerUUID, RewardEntry rewardEntry) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT IGNORE INTO rewards (id, playerUUID, reward) VALUES (?, ?, ?)", // two joins racing both insert
                    rewardEntry.getId(),
                    playerUUID,
                    rewardEntry.getReward()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // true only for the one call that actually flipped it, so a reward can't be claimed twice
    public boolean markRewardClaimed(String playerUUID, String rewardId) {
        try {
            return DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE rewards SET claimed = true WHERE playerUUID = ? AND id = ? AND claimed = false LIMIT 1",
                    playerUUID,
                    rewardId
            ) == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void markRewardUnclaimed(String playerUUID, String rewardId) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE rewards SET claimed = false WHERE playerUUID = ? AND id = ? LIMIT 1",
                    playerUUID,
                    rewardId
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
