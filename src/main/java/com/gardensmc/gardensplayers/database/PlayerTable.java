package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                    questsCompleted int NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid),
                    INDEX usernameIdx (username)
               )
               """;
    }

    @Override
    public List<String> getMigrations() {
        return List.of(
                // added after launch; existing player tables won't have it from CREATE TABLE IF NOT EXISTS
                "ALTER TABLE player ADD COLUMN questsCompleted int NOT NULL DEFAULT 0"
        );
    }

    public PlayerEntry getPlayerEntry(UUID uuid) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    "SELECT *, " +
                            "(SELECT COUNT(*) + 1 FROM player AS ranking " +
                            " WHERE ranking.questsCompleted > player.questsCompleted) AS questRank " +
                            "FROM player " +
                            "LEFT JOIN lastLocation ON player.uuid = lastLocation.playerUUID " +
                            "LEFT JOIN wardrobes ON player.uuid = wardrobes.playerUUID " +
                            "WHERE uuid = ?",
                    PlayerEntry::new,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPlayerEntry(Player player) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                "INSERT INTO player (uuid, username, joined, money) VALUES (?, ?, ?, ?)",
                    player.getUniqueId().toString(),
                    player.getName(),
                    LocalDateTime.now(Clock.systemUTC()),
                    GardensPlayers.config.getStartingBalance()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUsername(UUID uuid, String username) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET username = ? WHERE uuid = ?",
                    username,
                    uuid.toString()
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

    public void addPoints(UUID uuid, int amount) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET points = points + ? WHERE uuid = ?",
                    amount,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void incrementQuestsCompleted(UUID uuid) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET questsCompleted = questsCompleted + 1 WHERE uuid = ?",
                    uuid.toString()
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

    // only succeeds when the balance covers it, so two concurrent debits can't overdraw; true if taken
    public boolean subtractMoney(UUID uuid, int amount) {
        try {
            return DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET money = money - ? WHERE uuid = ? AND money >= ?",
                    amount,
                    uuid.toString(),
                    amount
            ) == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // only succeeds when the balance covers it; true if taken
    public boolean subtractPoints(UUID uuid, int amount) {
        try {
            return DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET points = points - ? WHERE uuid = ? AND points >= ?",
                    amount,
                    uuid.toString(),
                    amount
            ) == 1;
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

    public void setPoints(String uuid, int points) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET points = ? WHERE uuid = ?",
                    points,
                    uuid
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMoney(String uuid, int money) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE player SET money = ? WHERE uuid = ?",
                    money,
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
                    "SELECT uuid FROM player WHERE LOWER(username) = ? LIMIT 1",
                    username.toLowerCase()
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

    /**
     * Finds usernames containing the given fragment (case-insensitive), for locating players whose exact name is
     * unknown. Exact matches sort first, then shorter (closer) matches, then alphabetically. Capped at {@code limit}.
     */
    public List<String> searchUsernames(String usernameFragment, int limit) {
        try {
            Connection conn = DatabaseConnector.INSTANCE.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username FROM player WHERE LOWER(username) LIKE ? ESCAPE '!' " +
                            "ORDER BY LOWER(username) = ? DESC, CHAR_LENGTH(username) ASC, username ASC LIMIT ?"
            );
            // Escape LIKE wildcards so underscores in usernames match literally. Use '!' as the escape char to
            // avoid MySQL treating a backslash escape char as escaping the surrounding string-literal quote.
            String escaped = usernameFragment.toLowerCase()
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_");
            ps.setString(1, "%" + escaped + "%");
            ps.setString(2, usernameFragment.toLowerCase());
            ps.setInt(3, limit);

            ResultSet rs = ps.executeQuery();
            var matches = new ArrayList<String>();
            while (rs.next()) {
                matches.add(rs.getString("username"));
            }
            rs.close();
            ps.close();
            conn.close();
            return matches;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<PlayerBal> getTopBalances() {
        try {
            Connection conn = DatabaseConnector.INSTANCE.getConnection();
            String query = "SELECT username, money FROM player ORDER BY money DESC LIMIT 50";
            PreparedStatement ps = conn.prepareStatement(query);

            ResultSet rs = ps.executeQuery();
            var topBalances = new ArrayList<PlayerBal>();
            while (rs.next()) {
                topBalances.add(new PlayerBal(rs.getString("username"), rs.getInt("money")));
            }
            rs.close();
            ps.close();
            conn.close();
            return topBalances;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record PlayerBal(String username, int balance) {
    }

    public List<QuestLeader> getTopQuestCompleters() {
        try {
            Connection conn = DatabaseConnector.INSTANCE.getConnection();
            String query = "SELECT uuid, username, questsCompleted FROM player " +
                    "WHERE questsCompleted > 0 ORDER BY questsCompleted DESC LIMIT 9";
            PreparedStatement ps = conn.prepareStatement(query);

            ResultSet rs = ps.executeQuery();
            var leaders = new ArrayList<QuestLeader>();
            while (rs.next()) {
                leaders.add(new QuestLeader(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getInt("questsCompleted")
                ));
            }
            rs.close();
            ps.close();
            conn.close();
            return leaders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record QuestLeader(UUID uuid, String username, int questsCompleted) {
    }
}
