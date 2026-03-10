package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.exception.NoPermissionException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class GamemodeCommand extends PlayerExecutorCommand {

    public GamemodeCommand() {
        super("gamemode", 1);
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        GameMode gameMode;
        try {
            gameMode = GameMode.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateException("Invalid gamemode: " + args[0]);
        }
        var permission = "gplayers.gamemode." + gameMode.toString().toLowerCase();
        if (!executor.hasPermission(permission)) {
            throw new NoPermissionException(permission);
        }
        executor.setGameMode(gameMode);
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set gamemode to " + gameMode.toString().toLowerCase()));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.stream(GameMode.values()).map(gameMode -> gameMode.toString().toLowerCase()).toList();
        }
        return super.onTabComplete(commandSender, command, s, args);
    }
}
