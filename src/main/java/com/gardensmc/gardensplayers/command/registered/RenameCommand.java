package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class RenameCommand extends PlayerExecutorCommand {

    public RenameCommand() {
        super("rename");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        var heldItem = executor.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            throw new IllegalStateException("You must be holding an item to rename");
        }
        if (args.length == 0) {
            throw new IllegalArgumentException("name");
        }
        var name = MessageUtil.fromMiniMessage(String.join(" ", args));
        if (MessageUtil.toPlainText(name).length() >= 50) {
            throw new IllegalStateException("Name is too long!");
        }
        var meta = heldItem.getItemMeta();
        meta.displayName(Component.text()
                .decoration(TextDecoration.ITALIC, false)
                .append(name)
                .build());
        heldItem.setItemMeta(meta);
        sendMessage(executor, Component.text().content("Renamed item to ").append(name).build());
    }
}
