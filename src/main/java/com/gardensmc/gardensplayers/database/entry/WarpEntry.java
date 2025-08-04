package com.gardensmc.gardensplayers.database.entry;

import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WarpEntry extends MySQLEntry implements Comparable<WarpEntry> {

    private int id;
    private UUID playerUUID;
    private String name;
    private LocationJsonEntry location;
    private Category category;
    private boolean publicAccess;
    private int visits;
    private boolean serverWarp;
    private String description;

    @Override
    public MySQLEntry populate(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        this.playerUUID = UUID.fromString(resultSet.getString("playerUUID"));
        this.name = resultSet.getString("name");
        this.location = LocationJsonEntry.fromJson(resultSet.getString("location"));
        this.category = Category.valueOf(resultSet.getString("category"));
        this.publicAccess = resultSet.getBoolean("publicAccess");
        this.visits = resultSet.getInt("visits");
        this.serverWarp = resultSet.getBoolean("serverWarp");
        this.description = resultSet.getString("description");
        return this;
    }

    public String getReadableName() {
        return PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(name));
    }

    @Override
    public int compareTo(@NotNull WarpEntry o) {
        if (serverWarp && !o.serverWarp) {
            return -1;
        }
        if (!serverWarp && o.serverWarp) {
            return 1;
        }
        if (visits != o.visits) {
            return Integer.compare(visits, o.visits) * -1; // larger visits should come first
        }
        return getReadableName().compareTo(o.getReadableName());
    }

    @AllArgsConstructor
    @Getter
    public enum Category {
        SHOP("Shops"),
        FARM("Farms"),
        MOB_FARM("Mob Farms"),
        LANDMARK("Landmarks"),
        BUILD("Builds"),
        GAME("Games"),
        MISC("Misc");

        private final String name;
    }
}
