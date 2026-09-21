package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MoreCommand extends PlayerExecutorCommand {

    public MoreCommand() {
        super("more");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        var item = executor.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }
        item.setAmount(item.getMaxStackSize());
        executor.sendMessage("Received more of held item");
    }
}
