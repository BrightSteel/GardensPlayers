package com.gardensmc.gardensplayers.command.type;

import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.exception.InvalidUserException;
import com.gardensmc.gardensplayers.command.exception.NoPermissionException;
import com.gardensmc.gardensplayers.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class PlayerExecutorCommand extends GardensCommand {

    public PlayerExecutorCommand(String name) {
        super(name);
    }

    public PlayerExecutorCommand(String name, int requiredArgs) {
        super(name, requiredArgs);
    }

    @Override
    protected void execute(CommandSender onlineUser, String[] args) {
        super.execute(onlineUser, args);
        if (args.length < requiredArgs) {
            throw new InvalidArgumentException();
        }
        if (onlineUser instanceof Player player) {
            execute(player, args);
        } else {
            throw new InvalidUserException();
        }
    }

    public void execute(Player executor, String[] args) {
        if (!executor.hasPermission(permissionNode)) {
            throw new NoPermissionException(permissionNode);
        }
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

