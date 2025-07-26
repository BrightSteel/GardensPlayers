package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand extends PlayerCommand {

    public FeedCommand() {
        super("feed");
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        feed(targetPlayer);
        sendMessage(executor, displayName(targetPlayer) + " has been fed!");
        sendMessage(targetPlayer, displayName(executor) + " has fed you!");
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        feed(player);
        sendMessage(player, "You have been fed!");
    }

    private void feed(Player player) {
        player.setFoodLevel(30);
        player.setSaturation(10);
        player.setExhaustion(0);
    }
}
