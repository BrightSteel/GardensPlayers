package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.MountsMenu;
import org.bukkit.entity.Player;

public class MountCommand extends PlayerExecutorCommand {

    public MountCommand() {
        super("mount");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        new MountsMenu(executor).open();
    }
}
