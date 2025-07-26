package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.Damageable;

public class RepairCommand extends PlayerExecutorCommand {

    public RepairCommand() {
        super("repair");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        var item = executor.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof Damageable damageable)) {
            throw new InvalidStateException("You must be holding an item with durability!");
        }
        damageable.setDamage(0);
        item.setItemMeta(damageable);
//        executor.getActiveItem().setItemMeta(itemMeta);
        sendMessage(executor, "Repaired your item");
    }
}
