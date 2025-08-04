package com.gardensmc.gardensplayers.command;

import com.gardensmc.gardensplayers.command.registered.*;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import org.bukkit.Bukkit;

import java.util.Objects;

public class Commands {

    public static void registerCommands() {
        register(new BalanceCommand());
        register(new PayCommand());
//        register(new NicknameCommand());
        register(new FeedCommand());
        register(new RepairCommand());
        register(new FlyCommand());
        register(new RenameCommand());
        register(new CraftCommand());
        register(new TopCommand());
        register(new AFKCommand());
        register(new RtpCommand());
        register(new FarmShopCommand());
        register(new GardensPlayersCommand());
        register(new WarpCommand());
    }

    private static void register(GardensCommand gardensCommand) {
        Objects.requireNonNull(Bukkit.getPluginCommand(gardensCommand.getName())).setExecutor(gardensCommand);
    }
}
