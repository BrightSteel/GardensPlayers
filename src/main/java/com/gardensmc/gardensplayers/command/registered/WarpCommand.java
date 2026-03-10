package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.menu.CreateWarpMenu;
import com.gardensmc.gardensplayers.menu.UpdateWarpMenu;
import com.gardensmc.gardensplayers.menu.WarpsMenu;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class WarpCommand extends PlayerExecutorCommand {

    public WarpCommand() {
        super("warp");
    }

    @Override
    public void execute(Player player, String[] args) {
        super.execute(player, args);
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "list" -> onListWarps(player);
                case "create" -> onCreateWarp(player, args);
                case "edit" -> onEditWarp(player, args);
//                case "setlocation" -> onSetLocation(player, args);
                case "delete" -> onDelete(player, args);
                default -> throw new InvalidStateException("Unknown argument: " + args[0]);
            }
        } else {
            onListWarps(player);
        }
    }

    private void onListWarps(Player player) {
        new WarpsMenu(
                player,
                WarpsHandler.INSTANCE.getCachedWarps().warpsByCategory().getOrDefault(
                    WarpEntry.Category.SHOP,
                    Collections.emptyList()),
                false,
                0
        ).open();
    }

    private void onCreateWarp(Player player, String[] args) {
        if (args.length < 2) {
            throw new InvalidStateException("Missing warp name argument");
        }
        var name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (name.length() > 100) {
            throw new InvalidStateException("Warp name too long!");
        }
        if (PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(name)).length() > 20) {
            throw new InvalidStateException("Warp name too long!");
        }
        new CreateWarpMenu(player, name).open();
    }

    private void onEditWarp(Player player, String[] args) {
        new UpdateWarpMenu(player, getWarpFromArgs(player, args)).open();
    }

//    private void onSetLocation(Player player, String[] args) {
//        var warp = getWarpFromArgs(player, args);
//        WarpsHandler.INSTANCE.updateWarpLocation(warp.getId(), player.getLocation()).thenRun(
//                () -> player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Updated warp location"))
//        );
//    }

    private void onDelete(Player player, String[] args) {
        var warp = getWarpFromArgs(player, args);
        WarpsHandler.INSTANCE.removeWarp(warp.getId()).thenRun(
                () -> player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Removed warp successfully"))
        );
    }

    private WarpEntry getWarpFromArgs(Player player, String[] args) {
        if (args.length < 2) {
            throw new InvalidStateException("Missing warp name argument");
        }
        var name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        var warps = player.hasPermission("gplayers.warp.edit.any")
                ? WarpsHandler.INSTANCE.getCachedWarps().warps()
                : WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
        return warps.stream()
                .filter(warpEntry -> warpEntry.getReadableName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new InvalidStateException("Could not find warp: " + name));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            return Collections.emptyList();
        }
        String filter = args[args.length - 1].toLowerCase();
        if (args.length == 1) {
            return Stream.of("list", "create", "edit", "delete").filter(s1 -> s1.contains(filter)).toList();
        }
        var warps = commandSender.hasPermission("gplayers.warp.edit.any")
                ? WarpsHandler.INSTANCE.getCachedWarps().warps()
                : WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
        var longFilter = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        return warps.stream()
                .map(WarpEntry::getReadableName)
                .filter(s1 -> s1.toLowerCase().contains(longFilter.toLowerCase()))
                .toList();
    }
}
