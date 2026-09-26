package com.gardensmc.gardensplayers.database;

import org.bukkit.entity.Player;

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
        DatabaseConnector.INSTANCE.update(
                "INSERT IGNORE INTO wardrobes (playerUUID, helmet, backpack, offhand, balloon) VALUES (?, ?, ?, ?, ?)",
                player.getUniqueId().toString(),
                null,
                null,
                null,
                null
        );
    }

    public void setHelmet(Player player, String helmet) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE wardrobes SET helmet = ? WHERE playerUUID = ? LIMIT 1",
                helmet,
                player.getUniqueId().toString()
        );
    }

    public void setBackpack(Player player, String backpack) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE wardrobes SET backpack = ? WHERE playerUUID = ? LIMIT 1",
                backpack,
                player.getUniqueId().toString()
        );
    }

    public void setOffhand(Player player, String offhand) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE wardrobes SET offhand = ? WHERE playerUUID = ? LIMIT 1",
                offhand,
                player.getUniqueId().toString()
        );
    }

    public void setBalloon(Player player, String balloon) {
        DatabaseConnector.INSTANCE.update(
                "UPDATE wardrobes SET balloon = ? WHERE playerUUID = ? LIMIT 1",
                balloon,
                player.getUniqueId().toString()
        );
    }
}
