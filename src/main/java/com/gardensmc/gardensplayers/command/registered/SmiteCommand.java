package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.entity.Player;

/**
 * /smite (alias /lightning) — ray traces from the player's eyes to the nearest block they're looking at and
 * strikes lightning at that spot.
 */
public class SmiteCommand extends PlayerExecutorCommand {

    private static final int MAX_DISTANCE = 100;

    public SmiteCommand() {
        super("smite");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);

        var rayTrace = executor.rayTraceBlocks(MAX_DISTANCE);
        if (rayTrace == null || rayTrace.getHitBlock() == null) {
            throw new InvalidStateException("You must be looking at a block within %d blocks".formatted(MAX_DISTANCE));
        }

        var target = rayTrace.getHitPosition().toLocation(executor.getWorld());
        executor.getWorld().strikeLightning(target);
    }
}
