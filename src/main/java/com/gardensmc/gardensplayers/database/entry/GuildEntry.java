package com.gardensmc.gardensplayers.database.entry;

import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * A guild: a named, persistent group of players with a shared bank balance.
 */
@Getter
public class GuildEntry extends MySQLEntry {

    private int id;
    private String name;
    @Nullable
    private String description;
    private int balance;
    @Nullable
    private LocationJsonEntry home;
    private LocalDateTime createdAt;

    @Override
    public GuildEntry populate(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        this.name = resultSet.getString("name");
        this.description = resultSet.getString("description");
        this.balance = resultSet.getInt("balance");
        var homeJson = resultSet.getString("home");
        if (homeJson != null) {
            this.home = LocationJsonEntry.fromJson(homeJson);
        }
        this.createdAt = resultSet.getObject("createdAt", LocalDateTime.class);
        return this;
    }
}
