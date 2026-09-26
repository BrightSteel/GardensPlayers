package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.database.entry.MySQLEntry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class DatabaseConnector {

    public static final DatabaseConnector INSTANCE = new DatabaseConnector();
    private final HikariDataSource dataSource;
    public HikariConfig hikariConfig = new HikariConfig();

    private DatabaseConnector() {
        ConfigurationSection configSection = Objects.requireNonNull(GardensPlayers.plugin.getConfig().getConfigurationSection("database"));
        String db = configSection.getString("databaseName");
        String host = configSection.getString("host");
        String port = configSection.getString("port");
        String username = configSection.getString("username");
        String password = configSection.getString("password");

        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(10);
        // log any connection held longer than this, with the stack that took it
        hikariConfig.setLeakDetectionThreshold(10_000);
        hikariConfig.setMinimumIdle(3);
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db;
        hikariConfig.setJdbcUrl(url);
        dataSource = new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // returns the number of rows changed. Connections and statements are always released, even when the
    // query fails - a failed query used to leak its connection until the pool ran dry.
    public int executeUpdate(String string, Object... obj) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = getPreparedStatement(connection, string, obj)) {
            return ps.executeUpdate();
        }
    }

    // the unchecked forms below are what the tables use: they run off the main thread, where a failed query
    // should fail the future rather than be handled on the spot

    public int update(String statement, Object... obj) {
        try {
            return executeUpdate(statement, obj);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public <T extends MySQLEntry> T queryOne(String query, Supplier<T> supplier, Object... obj) {
        try {
            return querySingleton(query, supplier, obj);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends MySQLEntry> List<T> queryList(String query, Supplier<T> supplier, Object... obj) {
        try {
            return queryCollection(query, supplier, obj);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement getPreparedStatement(Connection connection, String statement, Object... obj) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(statement);
        try {
            for (int i = 0; i < obj.length; i++) {
                ps.setObject(i + 1, obj[i]);
            }
        } catch (SQLException e) {
            ps.close();
            throw e;
        }
        return ps;
    }

    public <T extends MySQLEntry> T querySingleton(String query, Supplier<T> supplier, Object... obj) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement ps = getPreparedStatement(connection, query, obj);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            T mySQLEntry = supplier.get();
            mySQLEntry.populate(rs);
            return mySQLEntry;
        }
    }

    public <T extends MySQLEntry> List<T> queryCollection(String query, Supplier<T> supplier, Object... obj) throws SQLException {
        List<T> collection = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = getPreparedStatement(connection, query, obj);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                T entry = supplier.get();
                entry.populate(rs);
                collection.add(entry);
            }
        }
        return collection;
    }

    public void establishConnection() {
        try (var connection = getConnection()) {
            // just checks the database is reachable
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        dataSource.close();
    }
}
