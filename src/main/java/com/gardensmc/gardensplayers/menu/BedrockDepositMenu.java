package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.GardensPlayers;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Geyser/Bedrock equivalent of the Java {@link DepositGui}. Bedrock players can't render the
 * Java resource-pack deposit menu, so we send a simple Cumulus {@link CustomForm}: a slider whose
 * max is the number of golden acorns currently in the viewer's inventory, plus a submit button that
 * removes that many acorns and credits the economy. Rendered natively by Bedrock (no theming).
 */
public class BedrockDepositMenu {

    private static final String ACORN_ITEM_ID = "golden_acorn";

    private BedrockDepositMenu() {
    }

    public static void open(Player viewer) {
        int available = countAcorns(viewer);
        if (available <= 0) {
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have any acorns to deposit!"));
            return;
        }

        var form = CustomForm.builder()
                .title("Deposit Acorns")
                .slider("Acorns to deposit", 1, available, 1, available)
                .validResultHandler(response -> {
                    int requested = Math.max(1, Math.round(response.asSlider(0)));
                    // Run on the main thread — mutating the inventory and the economy off-thread is unsafe.
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> deposit(viewer, requested));
                })
                .build();
        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), form);
    }

    private static void deposit(Player viewer, int requested) {
        // Clamp to whatever the player actually has now, in case the inventory changed since the form opened.
        int toDeposit = Math.min(requested, countAcorns(viewer));
        if (toDeposit <= 0) {
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have any acorns to deposit!"));
            return;
        }
        removeAcorns(viewer, toDeposit);
        GardensPlayers.economy.depositPlayer(viewer, toDeposit);
        viewer.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>Deposited <yellow>" + toDeposit + "<green> acorns!"));
    }

    private static int countAcorns(Player viewer) {
        int count = 0;
        for (ItemStack item : viewer.getInventory().getContents()) {
            if (isAcorn(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static void removeAcorns(Player viewer, int amount) {
        var inventory = viewer.getInventory();
        int remaining = amount;
        var contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            var item = contents[slot];
            if (!isAcorn(item)) {
                continue;
            }
            int stackAmount = item.getAmount();
            if (stackAmount <= remaining) {
                inventory.setItem(slot, null);
                remaining -= stackAmount;
            } else {
                item.setAmount(stackAmount - remaining);
                remaining = 0;
            }
        }
    }

    private static boolean isAcorn(ItemStack item) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        return customItem != null && customItem.getIdentifier().equals(ACORN_ITEM_ID);
    }
}
