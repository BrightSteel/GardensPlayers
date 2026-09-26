package com.gardensmc.gardensplayers.database.entry;

import com.gardensmc.gardensplayers.util.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PlayerEntry extends MySQLEntry {

    private UUID uuid;
    private String username;
    private Component nickname;
    private LocalDateTime seen;
    private LocalDateTime joined;
    // volatile: adjusted in place from database threads, read on the main thread
    private volatile int money;
    private volatile int points;
    private int questsCompleted;
    /**
     * Leaderboard rank by quests completed (players with an equal count share a rank). Derived in the
     * {@code getPlayerEntry} query rather than stored as a column, so it's as fresh as the cached entry.
     */
    private int questRank;

    // foreign keyed
    @Nullable
    private LocationEntry lastLocation;
    private WardrobeEntry wardrobe;

    @Override
    public PlayerEntry populate(ResultSet resultSet) throws SQLException {
        this.uuid = UUID.fromString(resultSet.getString("uuid"));
        this.username = resultSet.getString("username");
        var nick = resultSet.getString("nickname");
        if (nick != null) {
            this.nickname = MessageUtil.fromMiniMessage(nick);
        }
        this.seen = resultSet.getObject("seen", LocalDateTime.class);
        this.joined = resultSet.getObject("joined", LocalDateTime.class);
        this.money = resultSet.getInt("money");
        this.points = resultSet.getInt("points");
        this.questsCompleted = resultSet.getInt("questsCompleted");
        this.questRank = resultSet.getInt("questRank");
        if (resultSet.getString("world") != null) {
            this.lastLocation = new LocationEntry().populate(resultSet);
        }
        this.wardrobe = new WardrobeEntry().populate(resultSet);
        return this;
    }

    // keep a cached entry in step with a balance change the database has already applied, instead of
    // throwing the entry away and reloading the whole row (with its joins and rank count) every time
    public synchronized void adjustMoney(int delta) {
        money += delta;
    }

    public synchronized void adjustPoints(int delta) {
        points += delta;
    }
}
