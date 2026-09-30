package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.menu.BedrockWarpEditMenu;
import com.gardensmc.gardensplayers.menu.BedrockWarpsMenu;
import com.gardensmc.gardensplayers.menu.CreateWarpMenu;
import com.gardensmc.gardensplayers.menu.UpdateWarpMenu;
import com.gardensmc.gardensplayers.util.BedrockUtil;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
                case "delete" -> onDelete(player, args);
                case "tp" -> onTpWarp(player, args);
                default -> throw new InvalidStateException("Unknown argument: " + args[0]);
            }
        } else {
            onListWarps(player);
        }
    }

    private void onListWarps(Player player) {
        if (BedrockUtil.isBedrock(player)) {
            BedrockWarpsMenu.open(player);
            return;
        }
        WarpsHandler.INSTANCE.openMenu(player);
    }

    private void onCreateWarp(Player player, String[] args) {
        if (args.length < 2) {
            throw new InvalidStateException("Missing warp name argument");
        }
        var name = WarpsHandler.cleanText(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        var error = WarpsHandler.validateName(name);
        if (error != null) {
            throw new InvalidStateException(error);
        }
        if (BedrockUtil.isBedrock(player)) {
            BedrockWarpEditMenu.openCreate(player, name);
            return;
        }
        new CreateWarpMenu(player, name).open();
    }

    private void onEditWarp(Player player, String[] args) {
        var warp = getWarpFromArgs(player, args);
        if (BedrockUtil.isBedrock(player)) {
            BedrockWarpEditMenu.openEdit(player, warp);
            return;
        }
        new UpdateWarpMenu(player, warp).open();
    }

    private void onTpWarp(Player player, String[] args) {
        // search every warp, not just owned ones; prefer an accessible match since names aren't unique across players
        var warp = findWarp(args, WarpsHandler.INSTANCE.getCachedWarps().warps().stream()
                .sorted(Comparator.comparing(warpEntry -> !WarpsHandler.INSTANCE.canPlayerAccess(player, warpEntry)))
                .toList());
        if (!WarpsHandler.INSTANCE.canPlayerAccess(player, warp)) {
            throw new InvalidStateException("This warp is not open to the public!");
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Warping to " + warp.getName()));
        WarpsHandler.INSTANCE.travel(player, warp);
    }

    private void onDelete(Player player, String[] args) {
        var warp = getWarpFromArgs(player, args);
        WarpsHandler.INSTANCE.removeWarp(warp.getId()).thenRun(
                () -> player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Removed warp successfully"))
        );
    }

    private WarpEntry getWarpFromArgs(Player player, String[] args) {
        var warps = player.hasPermission("gplayers.warp.edit.any")
                ? WarpsHandler.INSTANCE.getCachedWarps().warps()
                : WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
        return findWarp(args, warps);
    }

    private WarpEntry findWarp(String[] args, List<WarpEntry> warps) {
        if (args.length < 2) {
            throw new InvalidStateException("Missing warp name argument");
        }
        var name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
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
            return Stream.of("list", "create", "edit", "delete", "tp").filter(s1 -> s1.contains(filter)).toList();
        }
        List<WarpEntry> warps;
        if (args[0].equals("tp")) {
            warps = commandSender.hasPermission("gplayers.warp.any")
                    ? WarpsHandler.INSTANCE.getCachedWarps().warps().stream().limit(500).toList()
                    : WarpsHandler.INSTANCE.getCachedWarps().warps()
                        .stream()
                        .filter(warpEntry -> WarpsHandler.INSTANCE.canPlayerAccess(player, warpEntry))
                        .limit(500)
                        .toList();
        } else {
            warps = commandSender.hasPermission("gplayers.warp.edit.any")
                    ? WarpsHandler.INSTANCE.getCachedWarps().warps().stream().limit(500).toList()
                    : WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
        }
        var longFilter = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        return warps.stream()
                .map(WarpEntry::getReadableName)
                .filter(s1 -> s1.toLowerCase().contains(longFilter.toLowerCase()))
                .toList();
    }
}
