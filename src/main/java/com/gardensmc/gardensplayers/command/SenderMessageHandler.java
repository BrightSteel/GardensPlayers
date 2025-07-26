package com.gardensmc.gardensplayers.command;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.config.Config;
import com.gardensmc.gardensplayers.util.MessageUtil;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

@AllArgsConstructor
public class SenderMessageHandler {

    private final CommandSender onlineUser;
    private static final Config config = GardensPlayers.config;

    public void sendMessage(String message) {
        sendMessage(MessageUtil.parseIntoComponent(message));
    }

    public void sendError(String message) {
        sendError(MessageUtil.parseIntoComponent(message));
    }

    public void sendMessage(Component message) {
        onlineUser.sendMessage(Component.text()
                .content(config.getMessagePrefix() + " ")
                .color(NamedTextColor.GREEN)
                .append(message)
                .build()
        );
    }

    public void sendError(Component message) {
        onlineUser.sendMessage(Component.text()
                .content(config.getMessagePrefix() + " ")
                .color(NamedTextColor.RED)
                .append(message)
                .build()
        );
    }
}
