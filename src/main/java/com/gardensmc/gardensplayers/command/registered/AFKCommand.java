package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AFKCommand extends PlayerExecutorCommand {

    public AFKCommand() {
        super("afk");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        var playerData = OnlinePlayerCache.INSTANCE.getOnlinePlayerData(executor.getUniqueId());
        playerData.setAfkTick(0);
        playerData.setAFK(!playerData.isAFK());
        // update messages
        if (playerData.isAFK()) {
            sendMessage(executor, "You are now AFK");
            Bukkit.getServer().sendMessage(Component.text()
                    .content(displayName(executor) + " is now AFK")
                    .color(NamedTextColor.LIGHT_PURPLE)
            );
        } else {
            sendMessage(executor, "You are no longer AFK");
            Bukkit.getServer().sendMessage(Component.text()
                    .content(displayName(executor) + " is no longer AFK")
                    .color(NamedTextColor.LIGHT_PURPLE)
            );
        }
    }
}
