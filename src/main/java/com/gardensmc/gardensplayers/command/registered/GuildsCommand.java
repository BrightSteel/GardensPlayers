package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.guild.GuildsHandler;
import org.bukkit.entity.Player;

public class GuildsCommand extends PlayerExecutorCommand {

    public GuildsCommand() {
        super("guilds");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        GuildsHandler.INSTANCE.openMenu(executor);
    }
}
