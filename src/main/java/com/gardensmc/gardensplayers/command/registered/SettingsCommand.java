package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.exception.InvalidUserException;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.menu.SettingsMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /settings}: your settings, in {@link SettingsMenu}. Open to everyone. */
public class SettingsCommand extends GardensCommand {

    public SettingsCommand() {
        super("settings");
    }

    @Override
    public boolean isOpenToAll() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            throw new InvalidUserException();
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("HuskHomes")) {
            throw new InvalidStateException("Settings aren't available right now");
        }
        SettingsMenu.open(player);
    }
}
