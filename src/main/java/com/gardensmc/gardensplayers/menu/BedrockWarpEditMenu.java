package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

/**
 * Geyser/Bedrock equivalent of the Java {@code /warp create} and {@code /warp edit} editors
 * ({@link CreateWarpMenu} / {@link UpdateWarpMenu}, both {@link WarpMenu}s). It's a single Cumulus
 * {@link CustomForm} — warp name, category, visibility, and description — since the two screens are
 * identical apart from whether submitting creates a new warp (at the viewer's current location) or
 * updates an existing one. Rendered natively by Bedrock, so no resource-pack theming is involved.
 */
public class BedrockWarpEditMenu {

    private static final List<WarpEntry.Category> CATEGORIES = List.of(WarpEntry.Category.values());

    private BedrockWarpEditMenu() {
    }

    /** Create flow: a brand-new warp, pre-filled with the name typed after {@code /warp create}. */
    public static void openCreate(Player viewer, String warpName) {
        send(viewer, null, warpName, WarpEntry.Category.MISC, true, null);
    }

    /** Edit flow: an existing warp, pre-filled with its current metadata. */
    public static void openEdit(Player viewer, WarpEntry warp) {
        send(viewer, warp.getId(), warp.getName(), warp.getCategory(), warp.isPublicAccess(), warp.getDescription());
    }

    private static void send(Player viewer, Integer warpId, String name, WarpEntry.Category category,
                             boolean isPublic, String description) {
        boolean creating = warpId == null;
        // Component order is the contract with the response reads below: 0 name, 1 category, 2 public, 3 desc.
        var form = CustomForm.builder()
                .title(creating ? "Create Warp" : "Edit Warp")
                .input("Warp Name", "Enter a name", name == null ? "" : name)
                .dropdown("Category", CATEGORIES.stream().map(WarpEntry.Category::getName).toList(),
                        Math.max(0, CATEGORIES.indexOf(category)))
                .toggle("Public", isPublic)
                .input("Description", "Optional", description == null ? "" : description)
                .validResultHandler(response -> {
                    String newName = response.asInput(0).trim();
                    WarpEntry.Category newCategory = CATEGORIES.get(response.asDropdown(1));
                    boolean newPublic = response.asToggle(2);
                    String descInput = response.asInput(3).trim();
                    String newDescription = descInput.isEmpty() ? null : descInput;

                    String error = validateName(newName);
                    if (error != null) {
                        viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + error));
                        send(viewer, warpId, newName, newCategory, newPublic, newDescription);
                        return;
                    }
                    if (creating) {
                        create(viewer, newName, newCategory, newPublic, newDescription);
                    } else {
                        update(viewer, warpId, newName, newCategory, newPublic, newDescription);
                    }
                })
                .build();
        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), form);
    }

    /** Creates on the main thread — {@code createWarp} reads the viewer's location and permissions. */
    private static void create(Player viewer, String name, WarpEntry.Category category, boolean isPublic, String description) {
        Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            var result = WarpsHandler.INSTANCE.createWarp(viewer, name, viewer.getLocation(), category, isPublic, description);
            if (result.isSuccess()) {
                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Created warp " + name + "<green>!"));
            } else {
                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + result.message()));
            }
        });
    }

    private static void update(Player viewer, int warpId, String name, WarpEntry.Category category, boolean isPublic, String description) {
        WarpsHandler.INSTANCE.updateWarpMetadata(warpId, name, category, isPublic, description).whenComplete((ignored, error) ->
                viewer.sendMessage(MiniMessage.miniMessage().deserialize(error == null
                        ? "<green>Updated warp " + WarpsHandler.cleanText(name) + "<green>!"
                        : "<red>" + rootMessage(error))));
    }

    /** Mirrors the {@code /warp create} command's checks (raw <= 100, rendered <= 20, non-empty). */
    private static String validateName(String name) {
        return WarpsHandler.validateName(WarpsHandler.cleanText(name));
    }

    private static String rootMessage(Throwable error) {
        var cause = error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() != null ? cause.getMessage() : "Couldn't update the warp";
    }
}
