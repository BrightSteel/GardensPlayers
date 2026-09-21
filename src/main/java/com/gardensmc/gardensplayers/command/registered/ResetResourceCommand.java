package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.resourceworld.ResetResourceHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ResetResourceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("gplayers.resetresource")) {
            return false;
        }
        ResetResourceHandler.INSTANCE.triggerReset();
        sender.sendMessage("Resetting resource...");
        return true;
    }
}
