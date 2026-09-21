package com.gardensmc.gardensplayers.database.entry;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A pending request from a player to join a guild. Resolved against the {@code player} table for the requester's
 * username so requests can be displayed without per-row lookups. A request is consumed (deleted) when an owner or
 * officer accepts or denies it.
 */
@Getter
public class GuildJoinRequestEntry extends MySQLEntry {

    private int guildId;
    private UUID requesterUUID;
    private String requesterUsername;
    private LocalDateTime requestedAt;

    @Override
    public GuildJoinRequestEntry populate(ResultSet resultSet) throws SQLException {
        this.guildId = resultSet.getInt("guildId");
        this.requesterUUID = UUID.fromString(resultSet.getString("requesterUUID"));
        this.requesterUsername = resultSet.getString("requesterUsername");
        this.requestedAt = resultSet.getObject("requestedAt", LocalDateTime.class);
        return this;
    }
}
