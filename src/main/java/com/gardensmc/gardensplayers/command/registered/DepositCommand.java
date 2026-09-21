package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.command.type.PlayerCommand;
import com.gardensmc.gardensplayers.menu.BedrockDepositMenu;
import com.gardensmc.gardensplayers.menu.DepositGui;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class DepositCommand extends PlayerCommand {

    public DepositCommand() {
        super("deposit");
    }

    @Override
    public void executeOnSelf(Player player, String[] args) {
        super.executeOnSelf(player, args);
        openFor(player);
    }

    @Override
    public void executeOnPlayer(CommandSender executor, Player targetPlayer, String[] args) {
        super.executeOnPlayer(executor, targetPlayer, args);
        openFor(targetPlayer);
        executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Opened deposit gui for player: " + targetPlayer.getName()));
    }

    private void openFor(Player player) {
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            BedrockDepositMenu.open(player);
        } else {
            new DepositGui(player).open();
        }
    }
}
