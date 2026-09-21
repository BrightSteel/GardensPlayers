package com.gardensmc.gardensplayers.database;

import java.util.List;

public interface Table {
    String getPrepareStatement();

    /**
     * Idempotent schema migrations applied after the table is created, for columns added to an already-existing
     * table (since {@code CREATE TABLE IF NOT EXISTS} won't alter it). Failures from already-applied migrations
     * (e.g. duplicate column) are ignored by {@link Tables}.
     */
    default List<String> getMigrations() {
        return List.of();
    }
}
