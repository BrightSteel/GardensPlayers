package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.exception.InvalidUserException;
import com.gardensmc.gardensplayers.command.exception.NoPermissionException;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.menu.HomesMenu;
import com.gardensmc.gardensplayers.util.BedrockUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /homes} (and {@code /homelist}): your HuskHomes homes in {@link HomesMenu}. Anything after it (staff's
 * {@code /homelist <player>}), and Bedrock players, go to HuskHomes' own list. Open to whoever HuskHomes lets list
 * their homes.
 */
public class HomesCommand extends GardensCommand {

    private static final String PERMISSION = "huskhomes.command.homelist";

    public HomesCommand() {
        super("homes");
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
        if (!player.hasPermission(PERMISSION)) {
            throw new NoPermissionException(PERMISSION);
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("HuskHomes")) {
            throw new InvalidStateException("Homes aren't available right now");
        }
        if (args.length > 0 || BedrockUtil.isBedrock(player)) {
            player.performCommand(("huskhomes:homelist " + String.join(" ", args)).trim());
            return;
        }
        HomesMenu.open(player, 0);
    }
}
