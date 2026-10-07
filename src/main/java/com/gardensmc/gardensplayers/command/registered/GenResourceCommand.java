package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.resourceworld.GenResourceHandler;
import org.bukkit.World;
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
        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(GenResourceHandler.INSTANCE.describe());
            return true;
        }
        int count;
        World.Environment env;
        try {
            count = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
            env = (args.length > 1) ? World.Environment.valueOf(args[1].toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Usage: /genresource [count] [normal|nether|the_end] | status");
            return true;
        }
        if (count < 1) {
            sender.sendMessage("Count must be greater than 0");
            return true;
        }
        if (GenResourceHandler.INSTANCE.triggerGeneration(sender, count, env)) {
            sender.sendMessage("Started resource world generation");
        } else {
            sender.sendMessage("Generation already in progress!");
        }
        return true;
    }
}
