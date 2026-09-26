package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.exception.PlayerNotFoundException;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.database.entry.LocationEntry;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * /seen &lt;player&gt; — shows when a player was last online (or that they're currently online).
 * The main command requires no permission; the last-known location line is only shown to senders
 * with {@link #permissionNodeLocation}.
 */
public class SeenCommand extends GardensCommand {

    private final String permissionNodeLocation;

    public SeenCommand() {
        super("seen", 1);
        this.permissionNodeLocation = permissionNode + ".location";
    }

    @Override
    public void execute(CommandSender executor, String[] args) {
        // Intentionally no permission check here — the base /seen command is available to everyone.
        if (args.length < requiredArgs) {
            throw new InvalidArgumentException();
        }

        String username = args[0];
        var online = PlayerUtil.findOnline(username);
        UUID uuid = online != null ? online.getUniqueId() : PlayerUtil.getUUID(username);
        if (uuid == null) {
            throw new PlayerNotFoundException(username);
        }

        PlayerEntry entry = PlayerEntryCache.INSTANCE.getSync(uuid);
        if (entry == null) {
            throw new PlayerNotFoundException(username);
        }

        String name = online != null
                ? MessageUtil.toPlainText(online.displayName())
                : entry.getUsername();

        if (online != null) {
            // When the player is online, that's the only thing this command reports.
            executor.sendMessage(MessageUtil.parseIntoComponent(
                    "<green>%s is currently <bold>online</bold>".formatted(name)
            ));
            return;
        }

        if (entry.getSeen() != null) {
            executor.sendMessage(MessageUtil.parseIntoComponent(
                    "<yellow>%s was last seen %s".formatted(name, formatRelative(entry.getSeen()))
            ));
        } else {
            executor.sendMessage(MessageUtil.parseIntoComponent(
                    "<yellow>%s has not been seen before".formatted(name)
            ));
        }

        if (executor.hasPermission(permissionNodeLocation)) {
            sendLastLocation(executor, entry);
        }
    }

    private void sendLastLocation(CommandSender executor, PlayerEntry entry) {
        LocationEntry lastLocation = entry.getLastLocation();
        if (lastLocation == null) {
            return;
        }
        var location = lastLocation.toLocation();
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "unknown";
        executor.sendMessage(MessageUtil.parseIntoComponent(
                "<gray>Last location: %s at %d, %d, %d".formatted(
                        worldName,
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                )
        ));
    }

    /**
     * Formats a UTC timestamp as a coarse "... ago" string relative to now.
     */
    private static String formatRelative(LocalDateTime seen) {
        var duration = Duration.between(seen, LocalDateTime.now(Clock.systemUTC()));
        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }
        long days = duration.toDays();
        if (days > 0) {
            return pluralize(days, "day");
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return pluralize(hours, "hour");
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return pluralize(minutes, "minute");
        }
        return "moments ago";
    }

    private static String pluralize(long amount, String unit) {
        return "%d %s%s ago".formatted(amount, unit, amount == 1 ? "" : "s");
    }

    // doesn't check its permission node: available to everyone
    @Override
    public boolean isOpenToAll() {
        return true;
    }
}
