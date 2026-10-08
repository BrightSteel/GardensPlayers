package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.resourceworld.GenResourceHandler;
import com.gardensmc.gardensplayers.resourceworld.ResourceWorldStorage;
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
        if (GenResourceHandler.INSTANCE.isGenerating()) {
            sender.sendMessage("Generation already in progress!");
            return true;
        }
        // what's in storage counts toward the sets, so list it fresh first
        var storage = ResourceWorldStorage.INSTANCE;
        storage.refresh();
        storage.afterQueued(() -> {
            var make = GenResourceHandler.INSTANCE.triggerGeneration(sender, count, env);
            if (make == null) {
                sender.sendMessage("Generation already in progress!");
                return;
            }
            var parts = new java.util.ArrayList<String>();
            make.forEach((world, n) -> {
                if (n > 0) parts.add(n + " " + world);
            });
            sender.sendMessage(parts.isEmpty() ? "Nothing to generate"
                    : "Started resource world generation: " + String.join(", ", parts));
        });
        return true;
    }
}
