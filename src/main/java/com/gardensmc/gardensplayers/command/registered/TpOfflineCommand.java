package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.database.entry.PlayerEntry;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import org.bukkit.entity.Player;

/**
 * /tpoffline &lt;player&gt; — teleports the executor to a player's last known location. If the target is
 * online, the executor is teleported to them directly instead.
 */
public class TpOfflineCommand extends PlayerExecutorCommand {

    public TpOfflineCommand() {
        super("tpoffline", 1);
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);

        String username = args[0];
        var online = PlayerUtil.findOnline(username);
        if (online != null) {
            executor.teleport(online);
            executor.sendMessage(MessageUtil.parseIntoComponent(
                    "<green>Teleported to %s".formatted(MessageUtil.toPlainText(online.displayName()))
            ));
            return;
        }

        runAsync(executor, () -> PlayerUtil.loadOfflineEntry(username), entry -> teleportToLastLocation(executor, entry));
    }

    private void teleportToLastLocation(Player executor, PlayerEntry entry) {
        var lastLocation = entry.getLastLocation();
        if (lastLocation == null) {
            throw new InvalidStateException("%s has no known last location".formatted(entry.getUsername()));
        }
        var location = lastLocation.toLocation();
        if (location.getWorld() == null) {
            throw new InvalidStateException("%s's last location is in an unloaded world".formatted(entry.getUsername()));
        }

        executor.teleport(location);
        executor.sendMessage(MessageUtil.parseIntoComponent(
                "<green>Teleported to %s's last location".formatted(entry.getUsername())
        ));
    }
}
