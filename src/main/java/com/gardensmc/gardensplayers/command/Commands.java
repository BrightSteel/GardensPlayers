package com.gardensmc.gardensplayers.command;

import com.gardensmc.gardensplayers.command.registered.*;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import org.bukkit.Bukkit;

import java.util.Objects;

public class Commands {

    public static void registerCommands() {
        register(new BalanceCommand());
        register(new PayCommand());
//        register(new NicknameCommand());
        register(new FeedCommand());
        register(new RepairCommand());
        register(new FlyCommand());
        register(new RenameCommand());
        register(new CraftCommand());
        register(new TopCommand());
        register(new AFKCommand());
        register(new RtpCommand());
        register(new FarmShopCommand());
        register(new GardensPlayersCommand());
        register(new WarpCommand());
        register(new WithdrawCommand());
        register(new DepositCommand());
        register(new UpgradesCommand());
        register(new ShopCommand());
        register(new WardrobeCommand());
        register(new RewardsCommand());
        register(new SpawnerCommand());
        register(new TpposCommand());
        register(new MapCommand());
        register(new GamemodeCommand());
        register(new TrashCommand());
        register(new AnvilCommand());
        register(new LoomCommand());
        register(new GrindstoneCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("gem")).setExecutor(new CurrencyCommand(GemsCurrencyHandler.INSTANCE));
        Objects.requireNonNull(Bukkit.getPluginCommand("acorn")).setExecutor(new CurrencyCommand(AcornsCurrencyHandler.INSTANCE));
        Objects.requireNonNull(Bukkit.getPluginCommand("baltop")).setExecutor(new BalTopCommand());
    }

    private static void register(GardensCommand gardensCommand) {
        Objects.requireNonNull(Bukkit.getPluginCommand(gardensCommand.getName())).setExecutor(gardensCommand);
    }
}
