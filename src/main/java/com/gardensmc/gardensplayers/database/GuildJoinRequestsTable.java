package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.GuildJoinRequestEntry;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pending requests from players to join guilds. A row exists only while a request is outstanding; accepting or denying
 * it deletes the row. Rows are cascaded away if either the guild or the requesting player is removed.
 */
public class GuildJoinRequestsTable implements Table {

    /** Shared projection: every {@link GuildJoinRequestEntry} carries the requester's username via a join on the player table. */
    private static final String BASE_SELECT = """
            SELECT guildJoinRequests.guildId, guildJoinRequests.requesterUUID, guildJoinRequests.requestedAt,
                   player.username AS requesterUsername
            FROM guildJoinRequests
            JOIN player ON guildJoinRequests.requesterUUID = player.uuid
            """;

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS guildJoinRequests(
                    guildId int NOT NULL,
                    requesterUUID varchar(36) NOT NULL,
                    requestedAt datetime NOT NULL,
                    PRIMARY KEY (guildId, requesterUUID),
                    FOREIGN KEY (guildId) REFERENCES guilds(id) ON DELETE CASCADE,
                    FOREIGN KEY (requesterUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    INDEX requesterIdx (requesterUUID)
                )
               """;
    }

    public void addRequest(int guildId, UUID requesterUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO guildJoinRequests (guildId, requesterUUID, requestedAt) VALUES (?, ?, ?)",
                    guildId,
                    requesterUUID.toString(),
                    LocalDateTime.now(Clock.systemUTC())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public GuildJoinRequestEntry getRequest(int guildId, UUID requesterUUID) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    BASE_SELECT + "WHERE guildJoinRequests.guildId = ? AND guildJoinRequests.requesterUUID = ? LIMIT 1",
                    GuildJoinRequestEntry::new,
                    guildId,
                    requesterUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Pending requests to join the given guild, oldest first. */
    public List<GuildJoinRequestEntry> getRequestsForGuild(int guildId) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE guildJoinRequests.guildId = ? ORDER BY guildJoinRequests.requestedAt ASC",
                    GuildJoinRequestEntry::new,
                    guildId
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeRequest(int guildId, UUID requesterUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM guildJoinRequests WHERE guildId = ? AND requesterUUID = ? LIMIT 1",
                    guildId,
                    requesterUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Drops every pending request by a player — used once they join a guild, so stale requests to others don't linger. */
    public void removeAllRequestsBy(UUID requesterUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM guildJoinRequests WHERE requesterUUID = ?",
                    requesterUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
