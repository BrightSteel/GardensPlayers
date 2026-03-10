package com.gardensmc.gardensplayers.database;

import org.bukkit.entity.Player;

import java.sql.SQLException;

public class WardrobeTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
               CREATE TABLE IF NOT EXISTS wardrobes(
                    playerUUID varchar(36),
                    helmet text,
                    backpack text,
                    offhand text,
                    balloon text,
                    PRIMARY KEY (playerUUID),
                    FOREIGN KEY (playerUUID) REFERENCES player(uuid) ON DELETE CASCADE
               )
               """;
    }

    public void createWardrobeEntryIfNotExists(Player player) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT IGNORE INTO wardrobes (playerUUID, helmet, backpack, offhand, balloon) VALUES (?, ?, ?, ?, ?)",
                    player.getUniqueId().toString(),
                    null,
                    null,
                    null,
                    null
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setHelmet(Player player, String helmet) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE wardrobes SET helmet = ? WHERE playerUUID = ? LIMIT 1",
                    helmet,
                    player.getUniqueId().toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBackpack(Player player, String backpack) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE wardrobes SET backpack = ? WHERE playerUUID = ? LIMIT 1",
                    backpack,
                    player.getUniqueId().toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setOffhand(Player player, String offhand) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE wardrobes SET offhand = ? WHERE playerUUID = ? LIMIT 1",
                    offhand,
                    player.getUniqueId().toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBalloon(Player player, String balloon) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE wardrobes SET balloon = ? WHERE playerUUID = ? LIMIT 1",
                    balloon,
                    player.getUniqueId().toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
