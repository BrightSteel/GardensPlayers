package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.config.Config;
import com.gardensmc.gardensplayers.util.DataFiles;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

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
            GardensPlayers.plugin.reloadConfig();
            GardensPlayers.config = new Config(GardensPlayers.plugin.getConfig());
            DataFiles.loadYamls();
            com.gardensmc.gardensplayers.streak.StreakHandler.INSTANCE.reload();
            com.gardensmc.gardensplayers.title.Titles.INSTANCE.reload();
            try {
                GardensPlayers.loadShops();
                // storage settings may have just been filled in: list, upload and download as at startup
                var storage = com.gardensmc.gardensplayers.resourceworld.ResourceWorldStorage.INSTANCE;
                storage.refresh();
                storage.uploadFinished();
                storage.fillLocal();
                executor.sendMessage("Reloaded GardensPlayers");
            } catch (Exception e) {
                GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Reload failed", e);
                executor.sendMessage("Reload failed - see the console");
            }
        }
    }
}
