package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensplayers.database.Tables;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerUtil {

    @Nullable
    public static UUID getUUID(String username) {
        var promise = CompletableFuture.supplyAsync(() -> Tables.playerTable.getUUID(username));
        try {
            return promise.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
