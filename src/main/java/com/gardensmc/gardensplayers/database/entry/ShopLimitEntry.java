package com.gardensmc.gardensplayers.database.entry;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public class ShopLimitEntry extends MySQLEntry {

    private String shopItem;
    private int amount;
    private int day;

    @Override
    public ShopLimitEntry populate(ResultSet resultSet) throws SQLException {
        this.shopItem = resultSet.getString("shopItem");
        this.amount = resultSet.getInt("amount");
        this.day = resultSet.getInt("day");
        return this;
    }

}
