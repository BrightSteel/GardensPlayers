package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import com.gardensmc.gardensplayers.guild.GuildRole;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GuildMembersTable implements Table {

    /** Shared projection: every {@link GuildMemberEntry} carries the member's username via a join on the player table. */
    private static final String BASE_SELECT = """
            SELECT guildMembers.memberUUID, guildMembers.guildId, guildMembers.role, guildMembers.joinedAt,
                   player.username AS memberUsername
            FROM guildMembers
            JOIN player ON guildMembers.memberUUID = player.uuid
            """;

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS guildMembers(
                    memberUUID varchar(36) NOT NULL,
                    guildId int NOT NULL,
                    role varchar(20) NOT NULL DEFAULT 'MEMBER',
                    joinedAt datetime NOT NULL,
                    PRIMARY KEY (memberUUID),
                    FOREIGN KEY (guildId) REFERENCES guilds(id) ON DELETE CASCADE,
                    FOREIGN KEY (memberUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    INDEX guildIdx (guildId)
                )
               """;
    }

    public void addMember(UUID memberUUID, int guildId, GuildRole role) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO guildMembers (memberUUID, guildId, role, joinedAt) VALUES (?, ?, ?, ?)",
                    memberUUID.toString(),
                    guildId,
                    role.name(),
                    LocalDateTime.now(Clock.systemUTC())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** The guild a player belongs to (with their role), or {@code null} if they aren't in one. */
    @Nullable
    public GuildMemberEntry getMembership(UUID memberUUID) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    BASE_SELECT + "WHERE guildMembers.memberUUID = ? LIMIT 1",
                    GuildMemberEntry::new,
                    memberUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<GuildMemberEntry> getMembers(int guildId) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE guildMembers.guildId = ? " +
                            "ORDER BY FIELD(guildMembers.role, 'OWNER', 'ADMIN', 'OFFICER', 'MEMBER'), guildMembers.joinedAt ASC",
                    GuildMemberEntry::new,
                    guildId
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRole(UUID memberUUID, GuildRole role) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE guildMembers SET role = ? WHERE memberUUID = ? LIMIT 1",
                    role.name(),
                    memberUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeMember(UUID memberUUID) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM guildMembers WHERE memberUUID = ? LIMIT 1",
                    memberUUID.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
