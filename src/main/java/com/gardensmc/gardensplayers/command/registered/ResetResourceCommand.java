package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.resourceworld.ResetResourceHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** {@code /resetresource}: swap in the next pregenerated resource worlds. {@code /resetresource status}: what's left. */
public class ResetResourceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(ResetResourceHandler.PERMISSION)) {
            return false;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            ResetResourceHandler.INSTANCE.status(sender);
            return true;
        }
        sender.sendMessage("Resetting resource worlds...");
        ResetResourceHandler.INSTANCE.triggerReset(sender);
        return true;
    }
}
