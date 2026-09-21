package com.gardensmc.gardensplayers.menu;

import com.gardensmc.gardensplayers.database.PlayerTable.QuestLeader;
import com.gardensmc.gardensplayers.util.HeadUtil;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.entity.Player;

import java.util.List;

public class QuestLeaderboardMenu extends Menu {

    private static final String[] LAYOUT = new String[]{
            "      b  ",
            "         ",
            "    1    ",
            "   23    ",
            "   456   ",
            "   789   "
    };

    public QuestLeaderboardMenu(Player viewer, List<QuestLeader> leaders) {
        super(LAYOUT, "<font:gardens:default>" + getShift(-8) + "š" + getShift(-62) + "</font>", viewer);
        buildElements(leaders);
    }

    private void buildElements(List<QuestLeader> leaders) {
        gui.addElement(new StaticGuiElement('b', blankItem, click -> {
            open(() -> new QuestsMenu(viewer));
            return true;
        }, "<red>Back"));

        // slots '1'..'9' map to ranks 1..9
        for (int rank = 1; rank <= 9; rank++) {
            char slot = (char) ('0' + rank);
            if (rank <= leaders.size()) {
                gui.addElement(leaderElement(slot, rank, leaders.get(rank - 1)));
            } else {
                gui.addElement(new StaticGuiElement(slot, blankItem,
                        "<gray>#" + rank,
                        "<dark_gray>Unclaimed"
                ));
            }
        }
    }

    private StaticGuiElement leaderElement(char slot, int rank, QuestLeader leader) {
        return new StaticGuiElement(slot, HeadUtil.createHead(leader.uuid()),
                "<yellow>#" + rank + " <white>" + leader.username(),
                " ",
                "<gray>Quests completed: <white>" + leader.questsCompleted()
        );
    }
}
