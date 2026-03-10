package com.gardensmc.gardensplayers.database.entry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RewardEntry extends MySQLEntry {

    private String id;
    private String reward;
    private boolean claimed;

    @Override
    public MySQLEntry populate(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getString("id");
        this.reward = resultSet.getString("reward");
        this.claimed = resultSet.getBoolean("claimed");
        return this;
    }
}
