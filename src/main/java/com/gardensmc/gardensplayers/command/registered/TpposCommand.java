package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TpposCommand extends PlayerExecutorCommand {

    public TpposCommand() {
        super("tppos", 3);
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        var x = getCoord(args[0]);
        var y = getCoord(args[1]);
        var z = getCoord(args[2]);
        var location = new Location(executor.getWorld(), x, y, z);
        if (!executor.getWorld().getWorldBorder().isInside(location)) {
            throw new InvalidStateException("Coords must be inside world border!");
        }
        executor.teleport(location);
        executor.sendMessage(MiniMessage.miniMessage().deserialize("Teleported to %s %s %s".formatted(x, y, z)));
    }

    private double getCoord(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException ex) {
            throw new InvalidStateException("Coords should be numbers");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("x");
        }
        if (args.length == 2) {
            return List.of("y");
        }
        if (args.length == 3) {
            return List.of("z");
        }
        return super.onTabComplete(commandSender, command, s, args);
    }
}
