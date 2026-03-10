package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.util.ConfigUtil;
import org.bukkit.command.CommandSender;

public class GardensPlayersCommand extends GardensCommand {

    public GardensPlayersCommand() {
        super("gardensplayers");
    }

    @Override
    protected void execute(CommandSender executor, String[] args) {
        super.execute(executor, args);
        if (args.length == 0) {
            executor.sendMessage("GardensPlayers plugin");
        } else if (args[0].equalsIgnoreCase("reload")) {
            ConfigUtil.loadYamls();
            GardensPlayers.loadShops();
            executor.sendMessage("Reloaded GardensPlayers");
        }
    }
}
