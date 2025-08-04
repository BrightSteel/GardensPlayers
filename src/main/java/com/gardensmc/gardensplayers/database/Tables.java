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
