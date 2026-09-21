package com.gardensmc.gardensplayers.menu;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Geyser/Bedrock confirmation for {@code /deathback}: a Cumulus {@link ModalForm} with Yes/No buttons.
 * Confirming re-runs {@code /deathback confirm}, where the charge + teleport happen (see
 * {@link com.gardensmc.gardensplayers.command.registered.DeathBackCommand}).
 */
public class BedrockDeathBackMenu {

    private BedrockDeathBackMenu() {
    }

    public static void open(Player viewer, int cost) {
        var form = ModalForm.builder()
                .title("Travel Back")
                .content("Travel back to your death for " + cost + " acorns?")
                .button1("Yes")
                .button2("No")
                .validResultHandler(response -> {
                    if (response.clickedButtonId() == 0) {
                        viewer.chat("/deathback confirm");
                    }
                })
                .build();
        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), form);
    }
}
