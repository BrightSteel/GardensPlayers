package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.streak.StreakHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@code /streak}: your login streak, savers and the next rewards. Staff ({@code gplayers.streak.admin}) also have
 * {@code /streak <player>}, {@code set <player> <days>}, {@code credit <player>} (counts today, e.g. from the starter
 * quest's last step) and {@code reload}.
 */
public class StreakCommand extends GardensCommand {

    private static final String ADMIN = "gplayers.streak.admin";

    public StreakCommand() {
        super("streak");
    }

    @Override
    public boolean isOpenToAll() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        var handler = StreakHandler.INSTANCE;
        if (args.length > 0 && sender.hasPermission(ADMIN)) {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    handler.reload();
                    send(sender, "<green>Reloaded streaks.yml");
                    return;
                }
                case "set" -> {
                    var target = args.length > 2 ? Bukkit.getPlayerExact(args[1]) : null;
                    if (target == null) {
                        send(sender, "<red>Usage: /streak set <online player> <days>");
                        return;
                    }
                    handler.set(target, Integer.parseInt(args[2]));
                    send(sender, "<green>" + target.getName() + "'s streak is now " + args[2] + " days");
                    return;
                }
                case "credit" -> {
                    var target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
                    if (target == null) {
                        send(sender, "<red>Usage: /streak credit <online player>");
                        return;
                    }
                    boolean counted = handler.credit(target, "credited");
                    send(sender, counted ? "<green>Counted today for " + target.getName() : "<gray>Today already counted for " + target.getName());
                    return;
                }
                default -> {
                    var target = Bukkit.getPlayerExact(args[0]);
                    if (target != null) {
                        show(sender, target);
                        return;
                    }
                }
            }
        }
        if (sender instanceof Player player) {
            show(sender, player);
        } else {
            send(sender, "Usage: /streak <player> | set <player> <days> | credit <player> | reload");
        }
    }

    private static void show(CommandSender viewer, Player player) {
        var handler = StreakHandler.INSTANCE;
        var streak = handler.cached(player.getUniqueId());
        if (streak == null) {
            send(viewer, "<gray>Still loading the streak, try again in a moment.");
            return;
        }
        if (viewer != player) send(viewer, "<gray>" + player.getName() + ":");
        send(viewer, handler.status(player, streak));
        send(viewer, "<gray>Best streak: <white>" + streak.best() + " days <gray>· Streak savers: <white>" + streak.savers()
                + "<gray>/" + handler.config().maxSavers()
                + " <dark_gray>(one covers a missed day; earn one every " + handler.config().saverEvery() + " days in a row)");
        var upcoming = handler.upcoming(streak, 3);
        if (!upcoming.isEmpty()) {
            send(viewer, "<gray>Coming up:");
            for (var next : upcoming) {
                send(viewer, " <gray>Day <white>" + next.getKey() + "<gray>: " + StreakHandler.describe(next.getValue()));
            }
        }
    }

    private static void send(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN)) return List.of();
        if (args.length == 1) return List.of("set", "credit", "reload");
        return args.length == 2 ? super.onTabComplete(sender, command, s, args) : List.of();
    }
}
