package com.gardensmc.gardensplayers.command.type;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.exception.*;
import com.gardensmc.gardensplayers.util.MessageUtil;
import com.gardensmc.gardensplayers.util.PlayerUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

@Getter
public abstract class GardensCommand implements TabExecutor {

    private final String name;
    protected final String permissionNode;
    protected final int requiredArgs;

    public GardensCommand(String name) {
        this(name, 0);
    }

    /**
     * Commands that never check their permission node (everyone may use them) return true, so they're shown to
     * everyone. All others are only suggested to players holding the node.
     */
    public boolean isOpenToAll() {
        return false;
    }

    public GardensCommand(String name, int requiredArgs) {
        this.name = name;
        this.requiredArgs = requiredArgs;
        this.permissionNode = buildPermissionNode(name);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        SenderMessageHandler userMessageHandler = new SenderMessageHandler(commandSender);
        try {
            execute(commandSender, strings);
        } catch (InvalidUserException e) {
            if (e.getMessage() != null) {
                userMessageHandler.sendError(e.getMessage());
            } else {
                userMessageHandler.sendError("Console cannot perform this command!");
            }
        } catch (InvalidArgumentException e) {
            if (e.getArgumentName() != null) {
                userMessageHandler.sendError("Invalid argument provided for " + e.getArgumentName());
            } else {
                userMessageHandler.sendError(
                        "Invalid arguments, must be: " + command.getUsage().replace("<command>", command.getName())
                ); // not sure why, but getUsage doesn't do this replacement like /help does
            }
        } catch (PlayerNotFoundException e) {
            userMessageHandler.sendError(Component.text(String.format("Player '%s' could not be found", e.getUsername())));
        } catch (NoPermissionException e) {
            userMessageHandler.sendError("You do not have permission to use this command!");
        } catch (InvalidStateException e) {
            userMessageHandler.sendError(e.getReason());
        } catch (Exception e) {
            userMessageHandler.sendError("An internal server error occurred");
            GardensPlayers.plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to run command: " + command.getName(),
                    e
            );
        }
        return true;
    }

    /**
     * Executes command
     *
     * @throws NoPermissionException    if user does not have permission
     * @throws InvalidArgumentException if user-provided arguments are invalid
     * @throws InvalidUserException     if user cannot perform the command
     * @throws PlayerNotFoundException  if user-provided player cannot be found
     * @throws DatabaseException        if database interaction fails
     */
    protected void execute(CommandSender executor, String[] args) {
        if (executor instanceof Player player && !player.hasPermission(permissionNode)) {
            throw new NoPermissionException(permissionNode);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        String filter = args[args.length - 1];
        return Bukkit.getOnlinePlayers()
                .stream()
                .map(player -> MessageUtil.toPlainText(player.displayName()))
                .filter(displayName -> displayName.toLowerCase().contains(filter.toLowerCase()))
                .toList();
    }

    protected Player getPlayerOrThrow(String name) {
        var player = PlayerUtil.findOnline(name);
        if (player == null) {
            throw new PlayerNotFoundException(name);
        }
        return player;
    }

    private String buildPermissionNode(String name) {
        return "gplayers." + name;
    }
}
