package com.gardensmc.gardensplayers.database.entry;

import com.gardensmc.gardensplayers.streak.Streak;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StreakEntry extends MySQLEntry {

    private int current;
    private int best;
    private long lastDay;
    private int savers;

    @Override
    public MySQLEntry populate(ResultSet resultSet) throws SQLException {
        this.current = resultSet.getInt("current");
        this.best = resultSet.getInt("best");
        this.lastDay = resultSet.getLong("lastDay");
        this.savers = resultSet.getInt("savers");
        return this;
    }

    public Streak toStreak() {
        return new Streak(current, best, lastDay, savers);
    }
}
