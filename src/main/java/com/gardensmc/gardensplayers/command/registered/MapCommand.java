package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class MapCommand extends PlayerExecutorCommand {

    public MapCommand() {
        super("map");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<gray>View our Map: </gray><aqua><click:open_url:https://map.gardensmc.com>https://map.gardensmc.com</click>"));
    }
}
