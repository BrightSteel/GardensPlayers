package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.logging.Level;

public class Tables {

    public static final PlayerTable playerTable = register(PlayerTable::new);
    public static final LastLocationTable lastLocationTable = register(LastLocationTable::new);
    public static final ShopLimitsTable shopLimitsTable = register(ShopLimitsTable::new);
    public static final WarpTable warpTable = register(WarpTable::new);
    public static final WarpVisitsTable warpVisitsTable = register(WarpVisitsTable::new);
    public static final WardrobeTable wardrobeTable = register(WardrobeTable::new);
    public static final RewardsTable rewardsTable = register(RewardsTable::new);
    public static final StreakTable streakTable = register(StreakTable::new);
    public static final TitleTable titleTable = register(TitleTable::new);

    private static <T extends Table> T register(Supplier<T> tableSupplier) {
        var logger = GardensPlayers.plugin.getLogger();
        var table = tableSupplier.get();
        var tableName =  table.getClass().getSimpleName();
        try {
            // run on main thread as we should wait for initialization to complete
            DatabaseConnector.INSTANCE.executeUpdate(table.getPrepareStatement());
        } catch (SQLException e) {
            // 1005 errno 150: a new table's playerUUID can't reference player(uuid) when the database's default character
            // set has changed since player was made (an older database). Made again with player.uuid's own.
            var matched = e.getErrorCode() == 1005 ? matchPlayerUuid(table.getPrepareStatement()) : null;
            boolean retried = false;
            if (matched != null) {
                try {
                    DatabaseConnector.INSTANCE.executeUpdate(matched);
                    logger.info(tableName + ": made with player.uuid's character set, to match it");
                    retried = true;
                } catch (SQLException again) {
                    e.addSuppressed(again);
                }
            }
            if (!retried) {
                logger.log(Level.SEVERE, "Failed to initialize " + tableName, e);
                Bukkit.getServer().getPluginManager().disablePlugin(GardensPlayers.plugin);
                return table;
            }
        }
        for (String migration : table.getMigrations()) {
            try {
                DatabaseConnector.INSTANCE.executeUpdate(migration);
            } catch (SQLException e) {
                // MySQL/MariaDB 1060 = duplicate column, 1061 = duplicate index: the migration is already applied
                if (e.getErrorCode() != 1060 && e.getErrorCode() != 1061) {
                    logger.log(Level.WARNING, "Migration failed for " + tableName + ": " + migration, e);
                }
            }
        }
        return table;
    }

    /**
     * Load class into memory, ensuring tables are registered and set up prior to their first use.
     * Could skip this step but means there may be some delay when user firsts interacts with table.
     */
    public static void initializeClass() {
        GardensPlayers.plugin.getLogger().info("Initialized tables");
    }

    /**
     * The statement with its playerUUID column given player.uuid's character set and collation, or null when they
     * can't be read (or there's no such column to change).
     */
    private static String matchPlayerUuid(String statement) {
        if (!statement.contains("playerUUID varchar(36)")) return null;
        try (var connection = DatabaseConnector.INSTANCE.getConnection();
             var query = connection.prepareStatement(
                     "SELECT CHARACTER_SET_NAME, COLLATION_NAME FROM information_schema.COLUMNS "
                             + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'player' AND COLUMN_NAME = 'uuid'")) {
            var rows = query.executeQuery();
            if (!rows.next() || rows.getString(1) == null) return null;
            return statement.replaceFirst("playerUUID varchar\\(36\\)",
                    "playerUUID varchar(36) CHARACTER SET " + rows.getString(1) + " COLLATE " + rows.getString(2));
        } catch (SQLException e) {
            return null;
        }
    }

}
