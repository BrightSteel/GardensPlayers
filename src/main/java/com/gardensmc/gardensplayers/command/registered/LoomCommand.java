package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class LoomCommand extends PlayerExecutorCommand {

    public LoomCommand() {
        super("loom");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        executor.openInventory(Bukkit.createInventory(executor, InventoryType.LOOM));
    }
}
