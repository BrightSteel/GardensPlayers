package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.entity.Player;

public class TopCommand extends PlayerExecutorCommand {

    public TopCommand() {
        super("top");
    }

    @Override
    public void execute(Player player, String[] args) {
        super.execute(player, args);
        player.teleport(player
                .getWorld()
                .getHighestBlockAt(player.getLocation())
                .getLocation()
                .add(0, 1, 0)
        );
        sendMessage(player, "Teleported to top");
    }
}
