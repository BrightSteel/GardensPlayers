package com.gardensmc.gardensplayers.database.entry;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public class TitleEntry extends MySQLEntry {

    private String titleId;
    private boolean selected;

    @Override
    public MySQLEntry populate(ResultSet resultSet) throws SQLException {
        this.titleId = resultSet.getString("titleId");
        this.selected = resultSet.getBoolean("selected");
        return this;
    }
}
