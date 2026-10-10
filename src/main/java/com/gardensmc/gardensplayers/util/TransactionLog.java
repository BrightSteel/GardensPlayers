package com.gardensmc.gardensplayers.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.GardensPlayers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Every server-side purchase and sale, one CSV row each, in transactions/YYYY-MM-DD.csv (a file a day, Pacific time),
 * for working out prices and what's bought and sold. Written on one background thread, in order.
 * <p>
 * Columns: time (Pacific, ISO), player_uuid, player, source (shop, farm_shop, furniture_shop, enchanting_table),
 * category (the shop tab, or what kind of table spend), item, action (buy, sell, enchant, reroll), amount, unit_price,
 * total, currency (acorns, gems), xp (levels spent, enchanting table only).
 */
public final class TransactionLog {

    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    private static final String HEADER = "time,player_uuid,player,source,category,item,action,amount,unit_price,total,currency,xp\n";
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "GardensPlayers transaction log");
        thread.setDaemon(true);
        return thread;
    });

    private TransactionLog() {
    }

    public static void log(Player player, String source, @Nullable String category, String item, String action,
                           int amount, int unitPrice, int total, String currency, int xp) {
        var now = ZonedDateTime.now(ZONE);
        var row = String.join(",",
                now.toOffsetDateTime().toString(), player.getUniqueId().toString(), player.getName(),
                csv(source), csv(category == null ? "" : category), csv(item), action,
                String.valueOf(amount), String.valueOf(unitPrice), String.valueOf(total), currency, String.valueOf(xp)) + "\n";
        var file = GardensPlayers.plugin.getDataFolder().toPath().resolve("transactions")
                .resolve(now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv");
        WRITER.execute(() -> append(file, row));
    }

    /** What an item is, for the log: a custom item's id, otherwise its material (lower case). */
    public static String itemKey(@Nullable ItemStack item) {
        if (item == null) return "";
        var custom = GardensFurniture.itemRegistry.getCustomItem(item);
        if (custom != null) return custom.getIdentifier();
        return item.getType().getKey().getKey();
    }

    private static void append(Path file, String row) {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            Files.writeString(file, row, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            GardensPlayers.plugin.getLogger().log(Level.WARNING, "Couldn't write the transaction log", e);
        }
    }

    // quoted when it holds a comma, quote or line break
    private static String csv(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Writes what's still queued, at shutdown. */
    public static void flush() {
        WRITER.shutdown();
        try {
            WRITER.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
