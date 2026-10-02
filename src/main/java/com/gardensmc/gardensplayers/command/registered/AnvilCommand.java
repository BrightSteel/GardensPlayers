package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class AnvilCommand extends PlayerExecutorCommand {

    public AnvilCommand() {
        super("anvil");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        // a real anvil menu; Bukkit.createInventory(ANVIL) only looks like one and has no anvil logic
        MenuType.ANVIL.builder()
                .checkReachable(false)
                .build(executor)
                .open();
    }
}
