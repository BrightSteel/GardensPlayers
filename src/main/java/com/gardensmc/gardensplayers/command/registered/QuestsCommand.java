package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.menu.BedrockQuestsMenu;
import com.gardensmc.gardensplayers.menu.QuestsMenu;
import com.gardensmc.gardensplayers.util.BedrockUtil;
import org.bukkit.entity.Player;

public class QuestsCommand extends PlayerExecutorCommand {

    public QuestsCommand() {
        super("quests");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        if (BedrockUtil.isBedrock(executor)) {
            BedrockQuestsMenu.open(executor);
            return;
        }
        new QuestsMenu(executor).open();
    }
}
