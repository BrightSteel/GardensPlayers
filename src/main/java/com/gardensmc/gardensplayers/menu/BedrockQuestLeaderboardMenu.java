package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.cache.QuestLeaderboardCache;
import com.gardensmc.gardensplayers.database.PlayerTable.QuestLeader;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Geyser/Bedrock equivalent of the {@code /quests} leaderboard ({@link QuestLeaderboardMenu}), re-skinned
 * by the bundled Bedrock JSON UI pack (see {@code bedrock-resourcepack/}). Shows the top quest completers
 * as a 3×3 grid of player-head tiles (head icons loaded as image URLs from mc-heads.net, mirroring the
 * Java menu's player-head items). Read-only; reached from {@link BedrockQuestsMenu} via its Leaderboard
 * button and returns there via Back.
 *
 * <p><b>Plugin&lt;-&gt;pack contract.</b> The form title carries a hidden private-use glyph
 * ({@link #TOKEN}, rendered invisibly by {@code font/glyph_E1.png}) that gates the pack's
 * {@code gardens_qlb_form} layout. Buttons are sent in one fixed block so the pack's hardcoded cells map
 * to the right actions:
 * <ul>
 *   <li>{@code 0..SLOTS-1} — the ranked head tiles (hover-only, no click); empty ranks show a gray
 *       "Unclaimed" placeholder.</li>
 *   <li>{@code SLOTS} — the Back button (returns to the quests menu).</li>
 * </ul>
 * {@code #form_text} (the form content) drives the header line (the viewer's own rank).
 */
public class BedrockQuestLeaderboardMenu {

    private static final String TOKEN = "\uE115";
    private static final String DEFAULT_ICON = "textures/gardens/shop/default_item";

    // 3x3 ranked head tiles; keep in sync with the pack's hardcoded cells and QuestLeaderboardCache size.
    private static final int SLOTS = 9;

    private BedrockQuestLeaderboardMenu() {
    }

    public static void open(Player viewer) {
        var leaders = QuestLeaderboardCache.INSTANCE.getSync();
        var uuid = viewer.getUniqueId();
        var entry = PlayerEntryCache.INSTANCE.getSync(uuid);

        String header = "§fTop Quest Completers";
        if (entry != null) {
            header += "  §7·  §fYour rank: §e#" + entry.getQuestRank()
                    + "  §7·  §f" + entry.getQuestsCompleted() + " completed";
        }

        var builder = SimpleForm.builder()
                .title(TOKEN + "Quest Leaderboard")
                .content(header);

        // 0..SLOTS-1: ranked head tiles; empty ranks get a gray "Unclaimed" placeholder.
        for (int i = 0; i < SLOTS; i++) {
            int rank = i + 1;
            if (i < leaders.size()) {
                var leader = leaders.get(i);
                builder.button(tileText(rank, leader), FormImage.Type.URL, headUrl(leader.uuid()));
            } else {
                builder.button("§8#" + rank + "\n§8Unclaimed", FormImage.Type.PATH, DEFAULT_ICON);
            }
        }
        // SLOTS: Back (returns to the quests grid).
        builder.button("Back");

        builder.validResultHandler(response -> {
            if (response.clickedButtonId() == SLOTS) {
                BedrockQuestsMenu.open(viewer);
            }
        });

        FloodgateApi.getInstance().sendForm(uuid, builder.build());
    }

    /** Tile text: rank + name (top), then quests completed. Rank 1–3 are gold/silver/bronze. */
    private static String tileText(int rank, QuestLeader leader) {
        String rankColor = switch (rank) {
            case 1 -> "§6";
            case 2 -> "§7";
            case 3 -> "§c";
            default -> "§f";
        };
        String name = rankColor + "#" + rank + " §f" + leader.username();
        String countLine = "§7" + leader.questsCompleted() + " quests";
        return name + "\n" + countLine;
    }

    /** mc-heads.net rendered player head, used as the Bedrock tile icon (Cumulus URL image). */
    private static String headUrl(UUID uuid) {
        return "https://mc-heads.net/avatar/" + uuid;
    }
}
