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
    private Component username;
    private Component nickname;
    private LocalDateTime seen;
    private LocalDateTime joined;
    private int money;
    private int points;

    // foreign keyed
    @Nullable
    private LocationEntry lastLocation;

    @Override
    public PlayerEntry populate(ResultSet resultSet) throws SQLException {
        this.uuid = UUID.fromString(resultSet.getString("uuid"));
        this.username = MessageUtil.fromPlainText(resultSet.getString("username"));
        var nick = resultSet.getString("nickname");
        if (nick != null) {
            this.nickname = MessageUtil.fromMiniMessage(nick);
        }
        this.seen = resultSet.getObject("seen", LocalDateTime.class);
        this.joined = resultSet.getObject("joined", LocalDateTime.class);
        this.money = resultSet.getInt("money");
        this.points = resultSet.getInt("points");
        if (resultSet.getString("world") != null) {
            this.lastLocation = new LocationEntry().populate(resultSet);
        }
        return this;
    }
}
