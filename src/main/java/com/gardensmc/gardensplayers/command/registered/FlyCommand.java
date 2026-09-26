package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand extends PlayerCommand {

    public FlyCommand() {
        super("fly", 0, 1);
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        try {
            var mode = Mode.valueOf(args[0].toUpperCase());
            var result = setFlight(targetPlayer, mode);
            sendMessage(executor, "Set %s's flight to %s".formatted(
                    displayName(targetPlayer),
                    result.toString().toLowerCase())
            );
            sendMessage(targetPlayer, "%s set your flight to %s".formatted(
                    displayName(executor),
                    result.toString().toLowerCase()
            ));
        } catch (IllegalArgumentException e) {
            throw new InvalidArgumentException("mode");
        }
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        var mode = Mode.TOGGLE;
        if (args.length > 0) {
            try {
                mode = Mode.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                // not a mode: /fly <player> toggles that player's flight
                var target = PlayerUtil.findOnline(args[0]);
                if (target == null) {
                    throw new InvalidArgumentException("mode");
                }
                executeOnPlayer(player, target, new String[]{"toggle"});
                return;
            }
        }
        var result = setFlight(player, mode);
        sendMessage(player, "Set your flight to " + result.toString().toLowerCase());
    }

    private Mode setFlight(Player player, Mode mode) {
        switch (mode) {
            case ON -> player.setAllowFlight(true);
            case OFF -> player.setAllowFlight(false);
            case TOGGLE -> player.setAllowFlight(!player.getAllowFlight());
        }
        if (player.getAllowFlight()) {
            return Mode.ON;
        }
        return Mode.OFF;
    }

    private enum Mode {
        ON,
        OFF,
        TOGGLE
    }
}
