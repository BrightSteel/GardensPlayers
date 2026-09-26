package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensenchants.utils.ItemUtil;
import com.gardensmc.gardensplayers.command.exception.InvalidArgumentException;
import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.exception.NoPermissionException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.util.MessageUtil;
import de.tr7zw.nbtapi.NBT;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class SpawnerCommand extends PlayerExecutorCommand {

    public SpawnerCommand() {
        super("spawner", 2);
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);
        switch (args[0].toLowerCase()) {
            case "set" -> setSpawner(executor, args);
            case "get" -> getSpawner(executor, args);
            default -> throw new InvalidArgumentException(args[0].toLowerCase());
        }
    }

    private void getSpawner(Player executor, String[] args) {
        if (!executor.hasPermission("gplayers.spawner.give")) {
            throw new NoPermissionException("gplayers.spawner.give");
        }
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidArgumentException(args[1].toLowerCase());
        }
        var item = ItemUtil.buildSpawnerItem(entityType.toString().toLowerCase());
        executor.give(item);
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Received " + MessageUtil.readableId(entityType.toString().toLowerCase()) + " spawner"));
    }

    private void setSpawner(Player executor, String[] args) {
        if (!executor.hasPermission("gplayers.spawner.set")) {
            throw new NoPermissionException("gplayers.spawner.set");
        }
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidArgumentException(args[1].toLowerCase());
        }
        if (!executor.hasPermission("gplayers.spawner.set." + entityType.toString().toLowerCase())) {
            throw new InvalidStateException("You cannot set spawner type: " + MessageUtil.readableId(entityType.toString()));
        }
        var block = executor.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.SPAWNER) {
            throw new InvalidStateException("You must be looking at a spawner!");
        }
        setSpawnerType(block, entityType);
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set spawner to " + MessageUtil.readableId(entityType.toString())));
    }

    private void setSpawnerType(Block spawner, EntityType type) {
        NBT.modify(spawner.getState(), nbt -> {
            nbt.getOrCreateCompound("SpawnData").getOrCreateCompound("entity").setString("id", type.toString().toLowerCase());
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("set", "get");
        }
        if (args.length == 2) {
            var isSet = args[0].equalsIgnoreCase("set");
            return Arrays.stream(EntityType.values())
                    .map(type -> type.toString().toLowerCase())
                    .filter(s1 -> !isSet || commandSender.hasPermission("gplayers.spawner.set." + s1))
                    .toList();
        }
        return super.onTabComplete(commandSender, command, s, args);
    }
}
