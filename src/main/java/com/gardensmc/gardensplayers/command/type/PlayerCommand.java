package com.gardensmc.gardensplayers.command.type;

import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.exception.InvalidUserException;
import com.gardensmc.gardensplayers.command.exception.NoPermissionException;
import com.gardensmc.gardensplayers.command.exception.PlayerNotFoundException;
import com.gardensmc.gardensplayers.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * For commands following this format:
 * /<command> <...requiredArgs> [...optionalArgs] [optionalPlayer]
 */
public abstract class PlayerCommand extends GardensCommand {

    protected final String permissionNodeOthers;
    protected final int optionalArgs;

    public PlayerCommand(String name, int requiredArgs, int optionalArgs) {
        super(name, requiredArgs);
        this.optionalArgs = optionalArgs;
        this.permissionNodeOthers = buildPermissionNodeOthers(name);
    }

    public PlayerCommand(String name, int requiredArgs) {
        this(name, requiredArgs, 0);
    }

    public PlayerCommand(String name) {
        this(name, 0);
    }

    public void executeOnSelf(Player player, String[] args) {
        if (!player.hasPermission(permissionNode)) {
            throw new NoPermissionException(permissionNode);
        }
    }

    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        if (!executor.hasPermission(permissionNodeOthers)) {
            throw new NoPermissionException(permissionNodeOthers);
        }
    }

    @Override
    public void execute(CommandSender onlineUser, String[] args) {
        if (args.length < requiredArgs) {
            throw new InvalidArgumentException();
        } else if (args.length > requiredArgs + optionalArgs) { // player parameter was specified
            String username = args[requiredArgs + optionalArgs];
            // check if user specified themselves
            if (onlineUser instanceof Player sprouterPlayer && MessageUtil.toPlainText(sprouterPlayer.displayName()).equalsIgnoreCase(username)) {
                executeOnSelf(sprouterPlayer, args);
            } else {
                Player targetPlayer = Bukkit.getPlayer(username);
                if (targetPlayer == null) {
                    throw new PlayerNotFoundException(username);
                }
                executeOnPlayer(onlineUser, targetPlayer, args);
            }
        } else {
            if (onlineUser instanceof Player sprouterPlayer) {
                executeOnSelf(sprouterPlayer, args);
            } else {
                throw new InvalidUserException();
            }
        }
    }

    private String buildPermissionNodeOthers(String name) {
        return String.format("gplayers.%s.others", name);
    }

    protected void sendMessage(CommandSender user, String message) {
        new SenderMessageHandler(user).sendMessage(message);
    }

    protected void sendMessage(CommandSender user, Component message) {
        new SenderMessageHandler(user).sendMessage(message);
    }

    protected String displayName(CommandSender user) {
        if (user instanceof Player player) {
            return MessageUtil.toPlainText(player.displayName());
        }
        return "Console";
    }
}
