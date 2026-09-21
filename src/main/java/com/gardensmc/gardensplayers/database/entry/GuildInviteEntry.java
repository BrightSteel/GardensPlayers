package com.gardensmc.gardensplayers.database.entry;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A standing invitation for a player to join a guild, created by an officer (or above). The counterpart to a
 * {@link GuildJoinRequestEntry}: where a request is the player asking in, an invite is the guild asking them in. The
 * invited player accepts by running {@code /guild join} for that guild, which consumes the invite.
 */
@Getter
public class GuildInviteEntry extends MySQLEntry {

    private int guildId;
    private UUID invitedUUID;
    private String invitedUsername;
    private LocalDateTime invitedAt;

    @Override
    public GuildInviteEntry populate(ResultSet resultSet) throws SQLException {
        this.guildId = resultSet.getInt("guildId");
        this.invitedUUID = UUID.fromString(resultSet.getString("invitedUUID"));
        this.invitedUsername = resultSet.getString("invitedUsername");
        this.invitedAt = resultSet.getObject("invitedAt", LocalDateTime.class);
        return this;
    }
}
