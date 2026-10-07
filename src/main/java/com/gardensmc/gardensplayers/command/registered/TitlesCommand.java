package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.title.Titles;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** {@code /titles}: the titles you've earned; {@code /titles <title>} shows one before your name, {@code none} hides it. */
public class TitlesCommand extends PlayerExecutorCommand {

    public TitlesCommand() {
        super("titles");
    }

    @Override
    public boolean isOpenToAll() {
        return true;
    }

    @Override
    public void execute(Player player, String[] args) {
        var titles = Titles.INSTANCE;
        var uuid = player.getUniqueId();
        var owned = titles.owned(uuid);
        if (args.length == 0) {
            if (owned.isEmpty()) {
                send(player, "<gray>You haven't earned any titles yet. Keep a login streak going to earn your first.");
                return;
            }
            send(player, "<gray>Your titles <dark_gray>(/titles <name> to show one, /titles none to hide it)<gray>:");
            var selected = titles.selected(uuid);
            for (var id : owned) {
                send(player, " " + (id.equals(selected) ? "<green>✔ " : "<dark_gray>- ") + titles.look(id) + " <dark_gray>" + id);
            }
            return;
        }
        var wanted = args[0].toLowerCase();
        if (wanted.equals("none")) {
            titles.select(uuid, null);
            send(player, "<gray>Your title is hidden.");
        } else if (titles.select(uuid, wanted)) {
            send(player, "<gray>Now showing " + titles.look(wanted));
        } else {
            send(player, "<red>You don't have that title. <gray>/titles lists yours.");
        }
    }

    private static void send(Player player, String message) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return List.of();
        var options = new ArrayList<>(Titles.INSTANCE.owned(player.getUniqueId()));
        options.add("none");
        return options;
    }
}
