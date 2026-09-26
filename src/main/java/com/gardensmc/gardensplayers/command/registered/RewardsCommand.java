package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.RewardsMenu;
import org.bukkit.entity.Player;

public class RewardsCommand extends PlayerExecutorCommand {

    public RewardsCommand() {
        super("rewards");
    }

    @Override
    public void execute(Player executor, String[] args) {
        new RewardsMenu(executor).open();
    }

    // doesn't check its permission node: available to everyone
    @Override
    public boolean isOpenToAll() {
        return true;
    }
}
