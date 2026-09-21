package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.database.entry.WarpEntry;
import com.gardensmc.gardensplayers.warps.WarpsHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Geyser/Bedrock equivalent of the {@code /warp} list menu (the Java-side tile menu opened by
 * {@link com.gardensmc.gardensplayers.warps.WarpsHandler#openMenu}), re-skinned by the
 * bundled Bedrock JSON UI pack (see {@code bedrock-resourcepack/}). Bedrock players can't render the
 * Java resource-pack menu, so we send a Cumulus {@link SimpleForm} that the pack restyles into a grid
 * of player-head tiles — the head icons are pulled as image URLs from mc-heads.net (mirroring the Java
 * menu, which uses player-head items).
 *
 * <p><b>Plugin&lt;-&gt;pack contract.</b> The form title carries a hidden private-use glyph
 * ({@link #WARP_TOKEN}, rendered invisibly by {@code font/glyph_E1.png}) that gates the pack's
 * {@code gardens_warps_form} layout. Buttons are sent in one fixed block so the pack's hardcoded cells
 * map to the right actions (missing tiles/nav are sent as empty buttons so indices stay aligned):
 * <ul>
 *   <li>{@code 0..SLOTS-1} — paginated warp head tiles (click = travel).</li>
 *   <li>{@code SLOTS} — Prev, {@code SLOTS+1} — Next, {@code SLOTS+2} — the "Yours" toggle.</li>
 *   <li>{@code SLOTS+3 ..} — the category tabs, one per {@link #TABS} entry.</li>
 * </ul>
 */
public class BedrockWarpsMenu {

    private static final String WARP_TOKEN = "\uE113";
    private static final String NAV_PREV_ICON = "textures/gardens/shop/nav_prev";
    private static final String NAV_NEXT_ICON = "textures/gardens/shop/nav_next";

    // Grid is GRID_COLS x GRID_ROWS tiles per page; keep in sync with the pack's hardcoded cells.
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 3;
    private static final int SLOTS = GRID_COLS * GRID_ROWS;

    // Category tabs, in display order. Keep the count (7) in sync with the pack's tab row.
    private static final List<CategoryTab> TABS = List.of(
            new CategoryTab(WarpEntry.Category.SHOP, "Shops"),
            new CategoryTab(WarpEntry.Category.FARM, "Farms"),
            new CategoryTab(WarpEntry.Category.MOB_FARM, "Mobs"),
            new CategoryTab(WarpEntry.Category.BUILD, "Builds"),
            new CategoryTab(WarpEntry.Category.LANDMARK, "Marks"),
            new CategoryTab(WarpEntry.Category.GAME, "Games"),
            new CategoryTab(WarpEntry.Category.MISC, "Misc")
    );

    private record CategoryTab(WarpEntry.Category category, String label) {
    }

    private BedrockWarpsMenu() {
    }

    public static void open(Player viewer) {
        sendWarpsForm(viewer, WarpEntry.Category.SHOP, false, 0);
    }

    private static void sendWarpsForm(Player viewer, WarpEntry.Category category, boolean yours, int page) {
        var cached = WarpsHandler.INSTANCE.getCachedWarps();
        var warps = yours
                ? cached.warpsByPlayer().getOrDefault(viewer.getUniqueId(), Collections.emptyList())
                : cached.warpsByCategory().getOrDefault(category, Collections.emptyList());

        int totalPages = Math.max(1, (int) Math.ceil(warps.size() / (double) SLOTS));
        int p = Math.max(0, Math.min(page, totalPages - 1));
        int from = p * SLOTS;
        var pageWarps = warps.subList(from, Math.min(from + SLOTS, warps.size()));
        int count = pageWarps.size();
        boolean hasPrev = p > 0;
        boolean hasNext = p < totalPages - 1;

        // #form_text drives the footer line: page counter, plus a create-warp hint on the Yours page.
        String footer = "Page " + (p + 1) + " / " + totalPages;
        if (yours) {
            footer += "  §7·  Create your own with /warp create <name>";
        }
        var builder = SimpleForm.builder()
                .title(WARP_TOKEN + "Warps")
                .content(footer);

        // 0..SLOTS-1: warp head tiles (mc-heads URL icons), padded to keep indices aligned.
        for (int i = 0; i < SLOTS; i++) {
            if (i < count) {
                var warp = pageWarps.get(i);
                builder.button(tileText(warp, yours), FormImage.Type.URL, headUrl(warp.getPlayerUUID()));
            } else {
                builder.button("");
            }
        }
        // SLOTS, +1: Prev / Next (empty = no image, so the pack hides the tile).
        if (hasPrev) {
            builder.button("Prev", FormImage.Type.PATH, NAV_PREV_ICON);
        } else {
            builder.button("");
        }
        if (hasNext) {
            builder.button("Next", FormImage.Type.PATH, NAV_NEXT_ICON);
        } else {
            builder.button("");
        }
        // SLOTS+2: "Yours" toggle (always visible; colour reflects the active state).
        builder.button(yours ? "§aYours" : "§fYours");
        // SLOTS+3..: category tabs (active one highlighted green).
        for (CategoryTab tab : TABS) {
            boolean active = !yours && tab.category() == category;
            builder.button((active ? "§a" : "§f") + tab.label());
        }

        builder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx < SLOTS) {
                if (idx < count) {
                    var warp = pageWarps.get(idx);
                    // On the Yours tab clicking offers Warp/Edit; elsewhere it just travels.
                    if (yours) {
                        sendWarpChooser(viewer, warp);
                    } else {
                        travel(viewer, warp);
                    }
                }
            } else if (idx == SLOTS) {
                if (hasPrev) {
                    sendWarpsForm(viewer, category, yours, p - 1);
                }
            } else if (idx == SLOTS + 1) {
                if (hasNext) {
                    sendWarpsForm(viewer, category, yours, p + 1);
                }
            } else if (idx == SLOTS + 2) {
                sendWarpsForm(viewer, category, true, 0);
            } else {
                int tab = idx - (SLOTS + 3);
                if (tab >= 0 && tab < TABS.size()) {
                    sendWarpsForm(viewer, TABS.get(tab).category(), false, 0);
                }
            }
        });

        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), builder.build());
    }

    /**
     * Warp/Edit chooser shown when a player taps one of their own warps on the Yours tab, so a single
     * tap can still travel while also exposing the editor. Plain title (no {@link #WARP_TOKEN}) so the
     * pack leaves it as a normal Bedrock form.
     */
    private static void sendWarpChooser(Player viewer, WarpEntry warp) {
        var form = SimpleForm.builder()
                .title(warp.getReadableName())
                .content("What would you like to do?")
                .button("Warp to it")
                .button("Edit")
                .validResultHandler(response -> {
                    if (response.clickedButtonId() == 0) {
                        travel(viewer, warp);
                    } else {
                        BedrockWarpEditMenu.openEdit(viewer, warp);
                    }
                })
                .build();
        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), form);
    }

    /** Teleports on the main thread (the form response fires async, teleport must be sync). */
    private static void travel(Player viewer, WarpEntry warp) {
        if (!WarpsHandler.INSTANCE.canPlayerAccess(viewer, warp)) {
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>This warp is not open to the public!"));
            return;
        }
        Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
            viewer.sendMessage(MiniMessage.miniMessage().deserialize("<green>Warping to " + warp.getName()));
            WarpsHandler.INSTANCE.travel(viewer, warp);
        });
    }

    /**
     * Tile text: the warp name (shown under the head, wrapping to up to two lines), then a blank line,
     * then the hover-only details (public/private + visits for your own warps, otherwise the owner +
     * description). The blank line keeps the details out of the tile's two-line name clip — the pack
     * shows only the name on the tile and reveals the whole string on hover.
     */
    private static String tileText(WarpEntry warp, boolean yours) {
        var details = new StringBuilder();
        if (yours) {
            details.append(warp.isPublicAccess() ? "§aPublic" : "§cPrivate");
            details.append("\n§7").append(warp.getVisits()).append(" Unique Visits");
        } else if (warp.isServerWarp()) {
            details.append("§9Server Warp");
        } else {
            details.append("§7by ").append(ownerName(warp.getPlayerUUID()));
        }
        if (warp.getDescription() != null && !warp.getDescription().isEmpty()) {
            details.append("\n§7").append(plain(warp.getDescription()));
        }
        return warp.getReadableName() + "\n\n" + details;
    }

    private static String ownerName(UUID uuid) {
        var entry = PlayerEntryCache.INSTANCE.getSync(uuid);
        return entry != null ? entry.getUsername() : "Unknown";
    }

    /** mc-heads.net rendered player head, used as the Bedrock tile icon (Cumulus URL image). */
    private static String headUrl(UUID uuid) {
        return "https://mc-heads.net/avatar/" + uuid;
    }

    private static String plain(String miniMessageOrPlain) {
        return PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(miniMessageOrPlain));
    }
}
