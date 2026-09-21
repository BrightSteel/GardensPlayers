package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * /lookup &lt;partial-username&gt; — finds registered players whose username contains the given fragment,
 * for locating a player whose exact name is unknown.
 */
public class LookupCommand extends GardensCommand {

    private static final int MAX_RESULTS = 25;

    public LookupCommand() {
        super("lookup", 1);
    }

    @Override
    public void execute(CommandSender executor, String[] args) {
        super.execute(executor, args);

        String fragment = args[0];
        var matches = Tables.playerTable.searchUsernames(fragment, MAX_RESULTS);

        if (matches.isEmpty()) {
            executor.sendMessage(MessageUtil.parseIntoComponent(
                    "<yellow>No players found matching '%s'".formatted(fragment)
            ));
            return;
        }

        executor.sendMessage(MessageUtil.parseIntoComponent(
                "<green>Players matching '<white>%s</white>'%s:".formatted(
                        fragment,
                        matches.size() == MAX_RESULTS ? " <gray>(first %d)".formatted(MAX_RESULTS) : ""
                )
        ));
        executor.sendMessage(MessageUtil.parseIntoComponent(
                "<gray>%s".formatted(String.join("<white>,</white> ", matches))
        ));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // No suggestions — the whole point is to search names that aren't already known/online.
        return List.of();
    }
}
