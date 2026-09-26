package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.cache.ShopLimitKey;
import com.gardensmc.gardensplayers.database.entry.ShopLimitEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ShopLimitsTable implements Table {

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS shopLimits(
                    playerUUID varchar(36),
                    shopItem varchar(50),
                    amount int NOT NULL DEFAULT 0,
                    day int NOT NULL,
                    PRIMARY KEY (playerUUID, shopItem),
                    FOREIGN KEY (playerUUID) REFERENCES player(uuid) ON DELETE CASCADE
                )
               """;
    }

    @Nullable
    public ShopLimitEntry getShopLimit(ShopLimitKey shopLimitKey) {
        return DatabaseConnector.INSTANCE.queryOne(
                "SELECT * FROM shopLimits where playerUUID = ? AND shopItem = ? LIMIT 1",
                ShopLimitEntry::new,
                shopLimitKey.playerUUID().toString(),
                shopLimitKey.shopItem()
        );
    }

    public List<ShopLimitEntry> getShopLimits(UUID uuid, int day) {
        return DatabaseConnector.INSTANCE.queryList(
                "SELECT * FROM shopLimits where playerUUID = ? AND day = ?",
                ShopLimitEntry::new,
                uuid.toString(),
                day
        );
    }

    // Adds to the player's amount for `day` in one statement: resets it first when the stored row is from an
    // earlier day, and creates the row if there is none. Concurrent sells can't lose each other's increments.
    public void addShopLimit(ShopLimitKey shopLimitKey, int amount, int day) {
        DatabaseConnector.INSTANCE.update(
                """
                INSERT INTO shopLimits (playerUUID, shopItem, amount, day) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE amount = IF(day = VALUES(day), amount + VALUES(amount), VALUES(amount)), day = VALUES(day)
                """,
                shopLimitKey.playerUUID().toString(),
                shopLimitKey.shopItem(),
                amount,
                day
        );
    }
}
