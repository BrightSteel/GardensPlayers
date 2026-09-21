package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.GuildInviteEntry;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Standing invitations from guilds to players (the inverse of {@link GuildJoinRequestsTable}). A row exists while an
 * invite is outstanding; it's removed when the player accepts (by joining), or cascaded away if the guild or player is
 * removed.
 */
public class GuildInvitesTable implements Table {

    /** Shared projection: every {@link GuildInviteEntry} carries the invited player's username via a join on the player table. */
    private static final String BASE_SELECT = """
            SELECT guildInvites.guildId, guildInvites.invitedUUID, guildInvites.invitedAt,
                   player.username AS invitedUsername
            FROM guildInvites
            JOIN player ON guildInvites.invitedUUID = player.uuid
            """;

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS guildInvites(
                    guildId int NOT NULL,
                    invitedUUID varchar(36) NOT NULL,
                    invitedAt datetime NOT NULL,
                    PRIMARY KEY (guildId, invitedUUID),
                    FOREIGN KEY (guildId) REFERENCES guilds(id) ON DELETE CASCADE,
                    FOREIGN KEY (invitedUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    INDEX invitedIdx (invitedUUID)
                )
               """;
    }

    public void addInvite(int guildId, UUID invitedUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO guildInvites (guildId, invitedUUID, invitedAt) VALUES (?, ?, ?)",
                    guildId,
                    invitedUUID.toString(),
                    LocalDateTime.now(Clock.systemUTC())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public GuildInviteEntry getInvite(int guildId, UUID invitedUUID) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    BASE_SELECT + "WHERE guildInvites.guildId = ? AND guildInvites.invitedUUID = ? LIMIT 1",
                    GuildInviteEntry::new,
                    guildId,
                    invitedUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Outstanding invites the given player has received, newest last. */
    public List<GuildInviteEntry> getInvitesForPlayer(UUID invitedUUID) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE guildInvites.invitedUUID = ? ORDER BY guildInvites.invitedAt ASC",
                    GuildInviteEntry::new,
                    invitedUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeInvite(int guildId, UUID invitedUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM guildInvites WHERE guildId = ? AND invitedUUID = ? LIMIT 1",
                    guildId,
                    invitedUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Drops every invite a player holds — used once they join a guild, so stale invites don't linger. */
    public void removeAllInvitesFor(UUID invitedUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM guildInvites WHERE invitedUUID = ?",
                    invitedUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
