package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.BedrockRtpMenu;
import com.gardensmc.gardensplayers.rtp.RtpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class RtpCommand extends PlayerExecutorCommand {

    public RtpCommand() {
        super("rtp");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        if (args.length == 0) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(executor.getUniqueId())) {
                BedrockRtpMenu.open(executor);
            } else if (executor.getWorld().getName().contains("resource")) {
                executor.chat("/dm open resource_rtp");
            } else {
                executor.chat("/dm open rtp");
            }
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
                if (world == null || !RtpHandler.RTP_WORLDS.contains(world.getName())) {
                    throw new InvalidArgumentException("Unknown world: " + args[0]);
                }
                var permission = RtpHandler.requiredPermission(world);
                if (permission != null && !executor.hasPermission(permission)) {
                    sendMessage(executor, "<red>Visit that dimension once to unlock random teleports there");
                    return;
                }
                var location = RtpHandler.INSTANCE.getQueuedLocation(world);
                if (location == null) {
                    sendMessage(executor, "<red>Location queue is empty, please wait and try again");
                } else {
                    // the cooldown starts only once they actually teleport
                    if (!executor.hasPermission("gardens.rtp.bypass_cooldown")) {
                        playerData.setRtpCooldown(300);
                    }
                    sendMessage(executor, "<green>Randomly teleporting...");
                    executor.teleport(location);
                }
            }
        }
    }
}
