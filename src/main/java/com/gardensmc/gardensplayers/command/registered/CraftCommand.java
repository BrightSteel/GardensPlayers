package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.entity.Player;

public class CraftCommand extends PlayerExecutorCommand {

    public CraftCommand() {
        super("craft");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        executor.openWorkbench(executor.getLocation(), true);
    }
}
