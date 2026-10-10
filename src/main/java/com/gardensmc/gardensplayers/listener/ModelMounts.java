package com.gardensmc.gardensplayers.listener;

import com.ticxo.modelengine.api.ModelEngineAPI;
import org.bukkit.entity.Player;

/** Whether a player rides a ModelEngine mount. Only loaded when ModelEngine is installed. */
final class ModelMounts {

    private ModelMounts() {
    }

    static boolean isMounted(Player player) {
        return ModelEngineAPI.getMountPairManager().getController(player.getUniqueId()) != null;
    }
}
