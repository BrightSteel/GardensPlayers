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

    private static <T extends Table> T register(Supplier<T> tableSupplier) {
        var logger = GardensPlayers.plugin.getLogger();
        var table = tableSupplier.get();
        var tableName =  table.getClass().getSimpleName();
        try {
            // run on main thread as we should wait for initialization to complete
            DatabaseConnector.INSTANCE.executeUpdate(table.getPrepareStatement());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize " + tableName, e);
            Bukkit.getServer().getPluginManager().disablePlugin(GardensPlayers.plugin);
            return table;
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
}
