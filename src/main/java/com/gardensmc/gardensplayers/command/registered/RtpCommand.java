package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.cache.OnlinePlayerCache;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.rtp.RtpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Set;

public class RtpCommand extends PlayerExecutorCommand {

    public RtpCommand() {
        super("rtp");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        if (args.length == 0) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(executor.getUniqueId())) {
                FloodgateApi.getInstance().sendForm(executor.getUniqueId(), getRtpForm(executor));
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

    private static final String[] WORLDS_BY_ID = new String[]{
            "gardens",
                    "resource_world",
                    "gardens_nether",
                    "gardens_the_end"
    };

    private Form getRtpForm(Player viewer) {
        return SimpleForm.builder()
                .title("RTP")
                .button("Overworld", FormImage.Type.PATH, "textures/blocks/sapling_oak.png")
                .button("Resource World", FormImage.Type.PATH, "textures/blocks/cherry_sapling.png")
                .button("Nether", FormImage.Type.PATH, "textures/items/nether_wart.png")
                .button("End", FormImage.Type.PATH, "textures/items/chorus_fruit.png")
                .validResultHandler(response -> viewer.chat("/rtp " + WORLDS_BY_ID[response.clickedButtonId()]))
                .build();
    }
}
