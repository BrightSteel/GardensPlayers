package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.resourceworld.GenResourceHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GenResourceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("gplayers.genresource")) {
            return false;
        }
        var count = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
        if (count < 1) {
            sender.sendMessage("Count must be greater than 0");
            return true;
        }
        if (GenResourceHandler.INSTANCE.triggerGeneration(sender, count)) {
            sender.sendMessage("Started resource world generation");
        } else {
            sender.sendMessage("Generation already in progress!");
        }
        return true;
    }
}
