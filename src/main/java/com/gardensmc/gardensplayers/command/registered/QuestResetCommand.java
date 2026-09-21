package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.quest.QuestHandler;
import org.bukkit.command.CommandSender;

public class QuestResetCommand extends GardensCommand {

    public QuestResetCommand() {
        super("questreset");
    }

    @Override
    protected void execute(CommandSender executor, String[] args) {
        super.execute(executor, args); // enforces the gplayers.questreset permission
        QuestHandler.INSTANCE.generateDailyTasks();
        QuestHandler.INSTANCE.saveAsync();
        new SenderMessageHandler(executor).sendMessage("<green>Daily quests have been reset!");
    }
}
