package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.rtp.RtpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RtpCommand extends PlayerExecutorCommand {

    public RtpCommand() {
        super("rtp");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        if (args.length == 0) {
            executor.chat("/dm open rtp");
        } else {
            var playerData = OnlinePlayerCache.INSTANCE.getOnlinePlayerData(executor.getUniqueId());
            var rtpCooldown = playerData.getRtpCooldown();
            if (rtpCooldown > 0) {
                sendMessage(executor, "<red>You cannot RTP for %s more seconds".formatted(rtpCooldown / 20));
            } else {
                var world = Bukkit.getWorlds().
                        stream()
                        .filter(w -> w.getName().equalsIgnoreCase(args[0]))
                        .findAny()
                        .orElse(null);
                if (world == null) {
                    throw new InvalidArgumentException("Unknown world: " + args[0]);
                }
                if (!executor.hasPermission("gardens.rtp.bypass_cooldown")) {
                    playerData.setRtpCooldown(300);
                }
                var location = RtpHandler.INSTANCE.getQueuedLocation(world);
                if (location == null) {
                    sendMessage(executor, "<red>Location queue is empty, please wait and try again");
                } else {
                    sendMessage(executor, "<green>Randomly teleporting...");
                    executor.teleport(location);
                }
            }
        }
    }
}
