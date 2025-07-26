package com.gardensmc.gardensplayers.database.entry;

import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class MySQLEntry {

    public abstract MySQLEntry populate(ResultSet resultSet) throws SQLException;

    @SneakyThrows
    public MySQLEntry clone() {
        return (MySQLEntry) super.clone();
    }


}
