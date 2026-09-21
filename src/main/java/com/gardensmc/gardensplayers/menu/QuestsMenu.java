package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.cache.PlayerEntryCache;
import com.gardensmc.gardensplayers.cache.QuestLeaderboardCache;
import com.gardensmc.gardensplayers.quest.QuestHandler;
import com.gardensmc.gardensplayers.quest.Task;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Displays the 16 shared daily quests with the viewing player's own progress. Read-only — rewards are auto-granted
 * on completion, so there is nothing to click.
 */
public class QuestsMenu extends Menu {

    private static final String[] LAYOUT = new String[] {
            "bcee iiii",
            "         ",
            " aaa aaa ",
            " aaa aaa ",
            " aa   aa ",
            "         "
    };

    public QuestsMenu(Player viewer) {
        super(
                LAYOUT,
                "<font:gardens:default>" + getShift(-8) + "\u0160" + getShift(-62) + "</font>"
                +  "<font:gardens:tile_upper>" + timeUntilReset() + "</font>",
                viewer
        );
    }

    @Override
    protected void initializeElements() {
        gui.addElement(new StaticGuiElement('b', blankItem, click -> {
            gui.close();
            return true;
        }, "<red>Close"));

        var entry = PlayerEntryCache.INSTANCE.getSync(viewer.getUniqueId());
        gui.addElement(new StaticGuiElement('e', blankItem, click -> {
            open(() -> new QuestLeaderboardMenu(viewer, QuestLeaderboardCache.INSTANCE.getSync()));
            return true;
        },
                "<gray>Leaderboard",
                " ",
                "<yellow>Your position: <white>#" + entry.getQuestRank(),
                "<gray>Click to view"
        ));

        var completedToday = QuestHandler.INSTANCE.getCompletedToday(viewer.getUniqueId());
        var dailyTaskCount = QuestHandler.INSTANCE.getDailyTasks().size();
        gui.addElement(new StaticGuiElement('c', blankItem,
                "<gray>" + viewer.getName(),
                " ",
                "<yellow>Total tasks completed: <white>" + entry.getQuestsCompleted(),
                "<gray>Tasks completed today: <white>" + completedToday + "/" + dailyTaskCount
        ));

        gui.addElement(new StaticGuiElement('i', blankItem,
                "<yellow>Daily Quests",
                "<gray>Resets in <white>" + timeUntilReset(),
                "<gray>Complete tasks to earn rewards!"
        ));

        var group = new GuiElementGroup('a');
        for (Task task : QuestHandler.INSTANCE.getDailyTasks()) {
            group.addElement(makeTaskElement(task));
        }
        gui.addElement(group);
    }

    private GuiElement makeTaskElement(Task task) {
        var progress = QuestHandler.INSTANCE.getProgress(viewer.getUniqueId(), task.getId());
        int count = progress == null ? 0 : progress.getCount();
        boolean completed = progress != null && progress.isCompleted();

        var name = (completed ? "<green>✔ " : "<yellow>") + task.getDescription();
        var progressLine = "<gray>Progress: " + (completed ? "<green>" : "<white>")
                + count + "<gray>/<white>" + task.getTargetAmount();
        var rewardLine = "<gray>Reward: " + task.getReward().describe();
        var statusLine = completed ? "<green>Completed!" : "";

        return new StaticGuiElement('a', taskIcon(task, completed),
                name,
                progressLine,
                rewardLine,
                statusLine
        );
    }

    /**
     * Builds the icon for a task from its category's GardensFurniture item (id {@code "icon_<category>"}, e.g.
     * {@code "icon_mining"}). When the task is completed, {@code "_completed"} is appended to the id. Falls back
     * to the category's vanilla Material if the custom item can't be resolved.
     */
    private ItemStack taskIcon(Task task, boolean completed) {
        try {
            var icon = GardensFurniture.itemRegistry.buildItem(task.getCategory().getIconItemId(completed));
            if (icon != null) {
                return icon;
            }
        } catch (Exception ignored) {
            // fall through to the vanilla icon
        }
        return new ItemStack(task.getCategory().getIcon());
    }

    private static String timeUntilReset() {
        return QuestHandler.INSTANCE.getFormattedTimeUntilReset();
    }
}
