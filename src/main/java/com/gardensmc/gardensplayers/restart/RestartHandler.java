package com.gardensmc.gardensplayers.restart;

import com.gardensmc.gardensplayers.GardensPlayers;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Schedules an automatic server restart at midnight in {@link #RESTART_ZONE}, broadcasting countdown warnings to
 * all players before it happens. Recomputes the schedule after each restart so it stays correct across DST changes.
 */
public class RestartHandler {

    public static final RestartHandler INSTANCE = new RestartHandler();

    // server restarts at midnight in this zone
    private static final ZoneId RESTART_ZONE = ZoneId.of("America/Los_Angeles");

    // how long before the restart each warning is broadcast, longest lead first
    private static final List<Duration> WARNING_LEADS = List.of(
            Duration.ofMinutes(15),
            Duration.ofMinutes(10),
            Duration.ofMinutes(5),
            Duration.ofMinutes(1),
            Duration.ofSeconds(5)
    );

    /**
     * Schedules the countdown warnings and the restart itself for the upcoming midnight in {@link #RESTART_ZONE}.
     */
    public void scheduleNextRestart() {
        var timeUntilRestart = getTimeUntilRestart();
        for (var lead : WARNING_LEADS) {
            var untilWarning = timeUntilRestart.minus(lead);
            if (untilWarning.isNegative()) {
                continue;
            }
            Bukkit.getScheduler().runTaskLater(
                    GardensPlayers.plugin,
                    () -> broadcastWarning(lead),
                    toTicks(untilWarning)
            );
        }
        Bukkit.getScheduler().runTaskLater(
                GardensPlayers.plugin,
                this::restart,
                toTicks(timeUntilRestart)
        );
    }

    private void broadcastWarning(Duration lead) {
        Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize(
                "<gradient:#f6d365:#fda085><bold>Server Restart</bold></gradient> <gray>in <yellow>"
                        + formatLead(lead) + "<gray>."
        ));
    }

    private void restart() {
        Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize(
                "<red><bold>Restarting now...</bold>"
        ));
        Bukkit.restart();
    }

    /**
     * Time left before the next restart (until the next midnight in {@link #RESTART_ZONE}).
     */
    public Duration getTimeUntilRestart() {
        var now = ZonedDateTime.now(RESTART_ZONE);
        var nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(RESTART_ZONE);
        return Duration.between(now, nextMidnight);
    }

    private static long toTicks(Duration duration) {
        return Math.max(1, duration.toSeconds() * 20);
    }

    private static String formatLead(Duration lead) {
        var minutes = lead.toMinutes();
        if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        var seconds = lead.toSeconds();
        return seconds + (seconds == 1 ? " second" : " seconds");
    }
}
