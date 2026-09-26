package com.gardensmc.gardensplayers.util;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class BedrockUtil {

    private BedrockUtil() {
    }

    // a Bedrock player joined through Geyser/Floodgate, who gets forms instead of chest menus
    public static boolean isBedrock(Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }
}
