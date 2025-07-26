package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.player.PlayerEntryHandler;
import com.gardensmc.gardensplayers.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class NicknameCommand extends PlayerCommand {

    public NicknameCommand() {
        super("nickname", 1);
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        var nick = updateNickname(player, args);
        if (nick == null) {
            sendMessage(player, "Nickname removed");
        } else {
            sendMessage(player, Component.text().content("Set nickname to ").append(nick).build());
        }
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        var nick = updateNickname(targetPlayer, args);
        if (nick == null) {
            sendMessage(executor, "Removed %s's nickname".formatted(displayName(targetPlayer)));
            sendMessage(targetPlayer, displayName(executor) + " removed your nickname");
        } else {
            sendMessage(executor, Component.text()
                    .content("Set %s's nickname to ".formatted(displayName(targetPlayer)))
                    .append(nick)
                    .build());
            sendMessage(executor, Component.text()
                    .content(displayName(executor) + " set your nickname to ")
                    .append(nick)
                    .build()
            );
        }
    }

    @Nullable
    private Component updateNickname(Player player, String[] args) {
        if (args[0].equalsIgnoreCase("off")) {
            removeNickname(player);
            return null;
        } else {
            var nick = MessageUtil.parseIntoComponent(args[0]);
            setNickname(nick, player);
            return nick;
        }
    }

    private void removeNickname(Player player) {
        new PlayerEntryHandler(player.getUniqueId()).setNickname(null);
    }

    private void setNickname(Component nickname, Player player) {
        var encodedNickname = MessageUtil.toMiniMessage(nickname);
        if (encodedNickname.length() > 150 || MessageUtil.toPlainText(nickname).length() > 20) {
            throw new InvalidArgumentException("nickname. Reason: too long");
        }
        new PlayerEntryHandler(player.getUniqueId()).setNickname(encodedNickname);
    }
}
