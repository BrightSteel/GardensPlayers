package com.gardensmc.gardensplayers.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Java confirmation for {@code /deathback}: a server-side {@link Dialog} (the Java analog of the Bedrock
 * {@link BedrockDeathBackMenu} form). Confirming re-runs {@code /deathback confirm}, where the charge +
 * teleport happen (see {@link com.gardensmc.gardensplayers.command.registered.DeathBackCommand}).
 */
public class DeathBackConfirmMenu {

    private DeathBackConfirmMenu() {
    }

    public static void open(Player viewer, int cost) {
        var yes = ActionButton.builder(Component.text("Yes"))
                .tooltip(Component.text("Travel back for %d acorns".formatted(cost)))
                .action(DialogAction.staticAction(ClickEvent.runCommand("/deathback confirm")))
                .build();
        // No action → the button simply dismisses the dialog.
        var no = ActionButton.builder(Component.text("No")).build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Travel back to death?"))
                        .body(List.of(DialogBody.plainMessage(
                                Component.text("Travel back to your death for %d acorns?".formatted(cost))
                        )))
                        .canCloseWithEscape(true)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build())
                .type(DialogType.confirmation(yes, no)));

        viewer.showDialog(dialog);
    }
}
