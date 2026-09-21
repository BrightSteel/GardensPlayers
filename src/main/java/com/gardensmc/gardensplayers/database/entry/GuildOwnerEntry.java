package com.gardensmc.gardensplayers.database.entry;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A guild paired with its owner (the member with the {@code OWNER} role), resolved in a single joined query so a list
 * of guilds can be rendered without per-guild lookups.
 */
@Getter
public class GuildOwnerEntry extends MySQLEntry {

    private GuildEntry guild;
    private UUID ownerUUID;
    private String ownerUsername;

    @Override
    public GuildOwnerEntry populate(ResultSet resultSet) throws SQLException {
        this.guild = new GuildEntry().populate(resultSet);
        this.ownerUUID = UUID.fromString(resultSet.getString("ownerUUID"));
        this.ownerUsername = resultSet.getString("ownerUsername");
        return this;
    }
}
