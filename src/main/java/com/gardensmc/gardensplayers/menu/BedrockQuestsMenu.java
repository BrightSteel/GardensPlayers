package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.quest.QuestHandler;
import com.gardensmc.gardensplayers.quest.Task;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Geyser/Bedrock equivalent of the {@code /quests} menu ({@link QuestsMenu}), re-skinned by the bundled
 * Bedrock JSON UI pack (see {@code bedrock-resourcepack/}). Bedrock players can't render the Java
 * resource-pack menu, so we send a Cumulus {@link SimpleForm} that the pack restyles into a fixed grid
 * of quest tiles (category icon + task name/progress on the tile, full details on hover).
 *
 * <p>The menu is read-only — rewards are auto-granted on completion, so the tiles do nothing on click
 * (the pack wires them to {@code button.menu_select}, a non-form action, so tapping a tile never closes
 * the form). Players close it with the frame's X.
 *
 * <p><b>Plugin&lt;-&gt;pack contract.</b> The form title carries a hidden private-use glyph
 * ({@link #QUEST_TOKEN}, rendered invisibly by {@code font/glyph_E1.png}) that gates the pack's
 * {@code gardens_quests_form} layout. Buttons are sent in one fixed block so the pack's hardcoded cells
 * map to the right actions (missing tiles/nav are padded with empty buttons so indices stay aligned):
 * <ul>
 *   <li>{@code 0..SLOTS-1} — this page's quest tiles (hover-only, no click).</li>
 *   <li>{@code SLOTS} — Prev, {@code SLOTS+1} — Next (form buttons; empty = hidden by the pack).</li>
 *   <li>{@code SLOTS+2} — the Leaderboard button (always visible; opens {@link BedrockQuestLeaderboardMenu}).</li>
 * </ul>
 * {@code #form_text} (the form content) drives the footer stats line.
 */
public class BedrockQuestsMenu {

    private static final String QUEST_TOKEN = "\uE114";
    private static final String DEFAULT_ICON = "textures/gardens/shop/default_item";
    private static final String NAV_PREV_ICON = "textures/gardens/shop/nav_prev";
    private static final String NAV_NEXT_ICON = "textures/gardens/shop/nav_next";
    private static final String ACORN_GLYPH = "\uE200";
    private static final String GEM_GLYPH = "\uE201";

    // Grid is GRID_COLS x GRID_ROWS tiles per page; keep in sync with the pack's hardcoded cells.
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 3;
    private static final int SLOTS = GRID_COLS * GRID_ROWS;

    private BedrockQuestsMenu() {
    }

    public static void open(Player viewer) {
        sendPage(viewer, 0);
    }

    private static void sendPage(Player viewer, int page) {
        var handler = QuestHandler.INSTANCE;
        var tasks = handler.getDailyTasks();
        var uuid = viewer.getUniqueId();

        int totalPages = Math.max(1, (int) Math.ceil(tasks.size() / (double) SLOTS));
        int p = Math.max(0, Math.min(page, totalPages - 1));
        int from = p * SLOTS;
        var pageTasks = tasks.subList(from, Math.min(from + SLOTS, tasks.size()));
        int count = pageTasks.size();
        boolean hasPrev = p > 0;
        boolean hasNext = p < totalPages - 1;

        int completedToday = handler.getCompletedToday(uuid);
        String footer = "§fCompleted: §a" + completedToday + "§7/§f" + tasks.size()
                + "  §7·  §fResets in §e" + handler.getFormattedTimeUntilReset();
        var streak = com.gardensmc.gardensplayers.streak.StreakHandler.INSTANCE.cached(uuid);
        if (streak != null) {
            footer += "  §7·  §6Streak §f" + streak.current();
        }
        if (totalPages > 1) {
            footer += "  §7·  §fPage §e" + (p + 1) + "§7/§f" + totalPages;
        }

        var builder = SimpleForm.builder()
                .title(QUEST_TOKEN + "Daily Quests")
                .content(footer);

        // 0..SLOTS-1: this page's quest tiles (padded with empty buttons to keep indices aligned).
        for (int i = 0; i < SLOTS; i++) {
            if (i < count) {
                var task = pageTasks.get(i);
                var progress = handler.getProgress(uuid, task.getId());
                int done = progress == null ? 0 : progress.getCount();
                boolean completed = progress != null && progress.isCompleted();
                builder.button(tileText(task, done, completed), FormImage.Type.PATH, icon(task, completed));
            } else {
                builder.button("");
            }
        }
        // SLOTS, +1: Prev / Next. Send an empty button (no image) when unavailable so the pack's
        // texture gate hides the tile entirely (otherwise the arrow renders with no label).
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
        // SLOTS+2: Leaderboard (always-visible text button).
        builder.button("Leaderboard");

        // Tiles are hover-only (button.menu_select) and never report; only Prev/Next/Leaderboard fire.
        builder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx == SLOTS && hasPrev) {
                sendPage(viewer, p - 1);
            } else if (idx == SLOTS + 1 && hasNext) {
                sendPage(viewer, p + 1);
            } else if (idx == SLOTS + 2) {
                BedrockQuestLeaderboardMenu.open(viewer);
            }
        });

        FloodgateApi.getInstance().sendForm(uuid, builder.build());
    }

    /**
     * Tile text: the task name and current progress (shown under the icon, clipped to three lines), then
     * a blank line, then the hover-only reward/status detail. The blank line keeps the details out of the
     * tile's clip — the pack shows only the name/progress on the tile and reveals everything on hover
     * (mirroring {@link BedrockWarpsMenu}'s tile pattern).
     */
    private static String tileText(Task task, int count, boolean completed) {
        String name = (completed ? "§a✔ " : "§f") + task.getDescription();
        String progressLine = (completed ? "§a" : "§7") + count + "§7/§f" + task.getTargetAmount();
        String rewardLine = "§7Reward:" + reward(task);
        String statusLine = completed ? "\n§aCompleted!" : "";

        // Two blank lines push the hover-only reward past the tile's 3-line clip (name + progress),
        // so it never peeks under a short (single-line) name.
        return name + "\n" + progressLine + "\n\n\n" + rewardLine + statusLine;
    }

    /** Reward text using the pack's currency glyphs for acorns/gems (see {@code font/glyph_E2.png}). */
    private static String reward(Task task) {
        int amount = task.getReward().amount();
        return switch (task.getReward().type()) {
            case ACORNS -> " §f" + ACORN_GLYPH + " §f" + amount;
            case GEMS -> " §f" + GEM_GLYPH + " §f" + amount;
            case XP -> " §a" + amount + " XP";
        };
    }

    private static String icon(Task task, boolean completed) {
        var path = QuestIcons.texturePath(task.getCategory(), completed);
        return path != null ? path : DEFAULT_ICON;
    }
}
