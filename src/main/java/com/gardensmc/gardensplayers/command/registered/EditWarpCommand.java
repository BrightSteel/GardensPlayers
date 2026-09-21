//package com.gardensmc.gardensplayers.command.registered;
//
//import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
//import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
//import com.gardensmc.gardensplayers.database.entry.WarpEntry;
//import com.gardensmc.gardensplayers.menu.UpdateWarpMenu;
//import com.gardensmc.gardensplayers.warps.WarpsHandler;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.jspecify.annotations.NonNull;
//
//import java.util.Collections;
//import java.util.List;
//
//public class EditWarpCommand extends PlayerExecutorCommand {
//
//    public EditWarpCommand() {
//        super("editwarp");
//    }
//
//    @Override
//    public void execute(Player executor, String[] args) {
//        super.execute(executor, args);
//        var argsStitched = String.join(" ", args);
//        var warp = getWarp(executor, argsStitched);
//        if (warp == null) {
//            throw new InvalidStateException("No warp exists with the name: " + argsStitched);
//        }
//        new UpdateWarpMenu(executor, getWarpFromArgs(player, args)).open();
//    }
//
//    private List<WarpEntry> getWarpsMatching(Player player, String beginsWith) {
//        var warps = WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
//        return warps.stream()
//                .filter(warpEntry -> warpEntry.getReadableName().toLowerCase().startsWith(beginsWith.toLowerCase()))
//                .toList();
//    }
//
//    private WarpEntry getWarp(Player player, String name) {
//        var warps = WarpsHandler.INSTANCE.getCachedWarps().warpsByPlayer().getOrDefault(player.getUniqueId(), Collections.emptyList());
//        return warps.stream()
//                .filter(warpEntry -> warpEntry.getReadableName().equalsIgnoreCase(name))
//                .findFirst()
//                .orElse(null);
//    }
//
//    @Override
//    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NonNull @NotNull String[] args) {
//        if (!(commandSender instanceof Player player)) {
//            return Collections.emptyList();
//        }
//        var argsStitched = String.join(" ", args);
//        return getWarpsMatching(player, argsStitched).stream().map(WarpEntry::getReadableName).toList();
//    }
//}
