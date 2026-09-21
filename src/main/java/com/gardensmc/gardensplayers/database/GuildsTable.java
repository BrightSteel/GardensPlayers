package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.GuildEntry;
import com.gardensmc.gardensplayers.database.entry.GuildOwnerEntry;
import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class GuildsTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS guilds(
                    id int NOT NULL AUTO_INCREMENT,
                    name varchar(32) NOT NULL,
                    description varchar(255),
                    balance int NOT NULL DEFAULT 0,
                    home json,
                    createdAt datetime NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY guildNameIdx (name)
                )
               """;
    }

    public void createGuild(String name, @Nullable String description) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO guilds (name, description, createdAt) VALUES (?, ?, ?)",
                    name,
                    description,
                    LocalDateTime.now(Clock.systemUTC())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public GuildEntry getGuildById(int id) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    "SELECT * FROM guilds WHERE id = ? LIMIT 1",
                    GuildEntry::new,
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public GuildEntry getGuildByName(String name) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    "SELECT * FROM guilds WHERE LOWER(name) = ? LIMIT 1",
                    GuildEntry::new,
                    name.toLowerCase()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rename(int id, String name) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE guilds SET name = ? WHERE id = ? LIMIT 1",
                    name,
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDescription(int id, @Nullable String description) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE guilds SET description = ? WHERE id = ? LIMIT 1",
                    description,
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addBalance(int id, int amount) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE guilds SET balance = balance + ? WHERE id = ? LIMIT 1",
                    amount,
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void subtractBalance(int id, int amount) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE guilds SET balance = balance - ? WHERE id = ? LIMIT 1",
                    amount,
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** All guilds paired with their owner, resolved in a single join (newest guilds aren't prioritised — sorted by name). */
    public List<GuildOwnerEntry> getGuildsWithOwners() {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    "SELECT guilds.*, owner.memberUUID AS ownerUUID, ownerPlayer.username AS ownerUsername " +
                            "FROM guilds " +
                            "JOIN guildMembers owner ON owner.guildId = guilds.id AND owner.role = 'OWNER' " +
                            "JOIN player ownerPlayer ON owner.memberUUID = ownerPlayer.uuid " +
                            "ORDER BY guilds.name ASC",
                    GuildOwnerEntry::new
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setHome(int id, LocationJsonEntry home) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE guilds SET home = ? WHERE id = ? LIMIT 1",
                    home.toJson(),
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteGuild(int id) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM guilds WHERE id = ? LIMIT 1",
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
