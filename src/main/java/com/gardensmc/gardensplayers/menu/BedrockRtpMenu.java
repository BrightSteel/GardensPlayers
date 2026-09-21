package com.gardensmc.gardensplayers.menu;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

/**
 * Geyser/Bedrock equivalent of the {@code /rtp} dimension picker, re-skinned by the bundled Bedrock
 * JSON UI pack (see {@code bedrock-resourcepack/}). Bedrock players can't render the Java DreamMinez
 * menu, so we send a Cumulus {@link SimpleForm} that the pack restyles into a framed window with two
 * text tabs — <b>Overworld</b> and <b>Resource</b> — each showing a row of three dimension cards.
 * Tapping a card runs {@code /rtp <world>} (the card art, incl. the green "+" button, is baked into
 * the tile icons under {@code textures/gardens/rtp/}). This replaces the old inline
 * {@code RtpCommand.getRtpForm}.
 *
 * <p><b>Plugin&lt;-&gt;pack contract.</b> The form title carries a hidden private-use glyph
 * ({@link #RTP_TOKEN}, rendered invisibly by {@code font/glyph_E1.png}) that gates the pack's
 * {@code gardens_rtp_form} layout. Buttons are sent in one fixed block so the pack's hardcoded cells
 * map to the right actions (missing cards are sent as empty buttons so indices stay aligned):
 * <ul>
 *   <li>{@code 0..SLOTS-1} — the dimension cards for the active tab (click = {@code /rtp <world>}).</li>
 *   <li>{@code SLOTS ..} — one text button per {@link #TABS} entry (active one highlighted green).</li>
 * </ul>
 */
public class BedrockRtpMenu {

    private static final String RTP_TOKEN = "\uE116";
    private static final String ICON_DIR = "textures/gardens/rtp/";

    /** Cards per tab; keep in sync with the pack's {@code card_row} cells (indices 0..SLOTS-1). */
    private static final int SLOTS = 3;

    private record Dimension(String world, String label, String icon) {
    }

    private record Tab(String label, List<Dimension> dimensions) {
    }

    // Tabs in display order; the pack's tab row has one cell per entry (indices SLOTS..SLOTS+size-1).
    private static final List<Tab> TABS = List.of(
            new Tab("Build", List.of(
                    new Dimension("gardens", "Overworld", ICON_DIR + "icon_overworld"),
                    new Dimension("gardens_nether", "Nether", ICON_DIR + "icon_nether"),
                    new Dimension("gardens_the_end", "End", ICON_DIR + "icon_end")
            )),
            new Tab("Resources", List.of(
                    new Dimension("resource_world", "Overworld", ICON_DIR + "icon_resource_world"),
                    new Dimension("resource_nether", "Nether", ICON_DIR + "icon_resource_nether"),
                    new Dimension("resource_the_end", "End", ICON_DIR + "icon_resource_end")
            ))
    );

    private BedrockRtpMenu() {
    }

    public static void open(Player viewer) {
        sendForm(viewer, 0);
    }

    private static void sendForm(Player viewer, int tabIndex) {
        int active = Math.max(0, Math.min(tabIndex, TABS.size() - 1));
        Tab tab = TABS.get(active);
        var dimensions = tab.dimensions();

        var builder = SimpleForm.builder().title(RTP_TOKEN + "RTP");

        // 0..SLOTS-1: the active tab's dimension cards, padded to keep indices aligned.
        for (int i = 0; i < SLOTS; i++) {
            if (i < dimensions.size()) {
                var dim = dimensions.get(i);
                builder.button(dim.label(), FormImage.Type.PATH, dim.icon());
            } else {
                builder.button("");
            }
        }
        // SLOTS..: the tabs (active one highlighted green).
        for (int t = 0; t < TABS.size(); t++) {
            builder.button((t == active ? "§a" : "§f") + TABS.get(t).label());
        }

        builder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx < SLOTS) {
                if (idx < dimensions.size()) {
                    viewer.chat("/rtp " + dimensions.get(idx).world());
                }
            } else {
                int t = idx - SLOTS;
                if (t >= 0 && t < TABS.size()) {
                    sendForm(viewer, t);
                }
            }
        });

        FloodgateApi.getInstance().sendForm(viewer.getUniqueId(), builder.build());
    }
}
