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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
        try {
            execute(commandSender, strings);
        } catch (Exception e) {
            handleError(commandSender, command, e);
        }
        return true;
    }

    /**
     * Runs {@code load} off the main thread (for database work), then {@code then} with its result back on the main
     * thread. Exceptions from either are reported to the sender like those thrown from {@link #execute}.
     */
    protected <T> void runAsync(CommandSender sender, Supplier<T> load, Consumer<T> then) {
        CompletableFuture.supplyAsync(load).whenComplete((result, error) -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            try {
                if (error != null) {
                    throw error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                }
                then.accept(result);
            } catch (Throwable e) {
                handleError(sender, null, e);
            }
        }));
    }

    // command is null for errors from runAsync, where the usage isn't at hand
    private void handleError(CommandSender commandSender, @Nullable Command command, Throwable error) {
        SenderMessageHandler userMessageHandler = new SenderMessageHandler(commandSender);
        switch (error) {
            case InvalidUserException e -> userMessageHandler.sendError(e.getMessage() != null ? e.getMessage() : "Console cannot perform this command!");
            case InvalidArgumentException e -> {
                if (e.getArgumentName() != null) {
                    userMessageHandler.sendError("Invalid argument provided for " + e.getArgumentName());
                } else if (command != null) {
                    userMessageHandler.sendError(
                            "Invalid arguments, must be: " + command.getUsage().replace("<command>", command.getName())
                    ); // not sure why, but getUsage doesn't do this replacement like /help does
                } else {
                    userMessageHandler.sendError("Invalid arguments");
                }
            }
            case PlayerNotFoundException e -> userMessageHandler.sendError(Component.text(String.format("Player '%s' could not be found", e.getUsername())));
            case NoPermissionException e -> userMessageHandler.sendError("You do not have permission to use this command!");
            case InvalidStateException e -> userMessageHandler.sendError(e.getReason());
            default -> {
                userMessageHandler.sendError("An internal server error occurred");
                GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to run command: " + name, error);
            }
        }
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
