package com.gardensmc.gardensplayers.database.entry;

import com.gardensmc.gardensplayers.guild.GuildRole;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A player's membership in a guild. A player can belong to at most one guild (enforced by the primary key on
 * {@code memberUUID}). The username is joined from the {@code player} table for display.
 */
@Getter
public class GuildMemberEntry extends MySQLEntry {

    private UUID memberUUID;
    private int guildId;
    private GuildRole role;
    private LocalDateTime joinedAt;
    private String memberUsername;

    @Override
    public GuildMemberEntry populate(ResultSet resultSet) throws SQLException {
        this.memberUUID = UUID.fromString(resultSet.getString("memberUUID"));
        this.guildId = resultSet.getInt("guildId");
        this.role = GuildRole.valueOf(resultSet.getString("role"));
        this.joinedAt = resultSet.getObject("joinedAt", LocalDateTime.class);
        this.memberUsername = resultSet.getString("memberUsername");
        return this;
    }
}
