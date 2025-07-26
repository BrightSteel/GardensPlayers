package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import com.gardensmc.gardensplayers.util.MessageUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS player(
                    uuid varchar(36),
                    username varchar(16) NOT NULL,
                    nickname varchar(150),
                    seen datetime,
                    joined datetime NOT NULL,
                    money int NOT NULL,
                    points int NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid),
                    INDEX usernameIdx (username)
               )
               """;
    }

    public PlayerEntry getPlayerEntry(UUID uuid) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    "SELECT * FROM player LEFT JOIN lastLocation ON player.uuid = lastLocation.playerUUID WHERE uuid = ?",
                    PlayerEntry::new,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPlayerEntry(Player player) {
        try {
            var username = player.displayName();
            DatabaseConnector.INSTANCE.executeUpdate(
                "INSERT INTO player (uuid, username, joined, money) VALUES (?, ?, ?, ?)",
                    player.getUniqueId().toString(),
                    MessageUtil.toPlainText(username),
                    LocalDateTime.now(Clock.systemUTC()),
                    GardensPlayers.config.getStartingBalance()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSeen(Player player, LocalDateTime time) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET seen = ? WHERE uuid = ?",
                    time,
                    player.getUniqueId().toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMoney(UUID uuid, int amount) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET money = money + ? WHERE uuid = ?",
                    amount,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void subtractMoney(UUID uuid, int amount) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET money = money - ? WHERE uuid = ?",
                    amount,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNickname(String uuid, String nickname) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET nickname = ? WHERE uuid = ?",
                    nickname,
                    uuid
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public UUID getUUID(String username) {
        try {
            Connection connection = DatabaseConnector.INSTANCE.getConnection();
            PreparedStatement ps = DatabaseConnector.INSTANCE.getPreparedStatement(
                    connection,
                    "SELECT uuid FROM player WHERE username = ? LIMIT 1",
                    username
            );
            ResultSet rs = ps.executeQuery();
            UUID uuid = null;
            if (rs.next()) {
                uuid = UUID.fromString(rs.getString("uuid"));
            }

            rs.close();
            ps.close();
            connection.close();
            return uuid;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
