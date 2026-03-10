package com.gardensmc.gardensplayers.command;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TrashCommand extends PlayerExecutorCommand {

    public TrashCommand() {
        super("trash");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        executor.openInventory(Bukkit.createInventory(executor, 27, MiniMessage.miniMessage().deserialize("Trash")));
    }
}
