package com.gardensmc.gardensplayers.command;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.registered.*;
import com.gardensmc.gardensplayers.command.type.GardensCommand;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.currency.GemsCurrencyHandler;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

public class Commands {

    /**
     * Registers all commands via Paper's Brigadier command system. Commands are no longer declared in plugin.yml;
     * their name, aliases, description, and usage are supplied here and bridged through {@link CommandBridge}.
     */
    public static void registerCommands() {
        GardensPlayers.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();

            register(registrar, new BalanceCommand(), "View yours or another players balance", "/<command> [player]", List.of("bal"));
            register(registrar, new PayCommand(), "Send money to another player", "/<command> <player> <amount>");
            register(registrar, new FeedCommand(), "Feed yourself or others", "/<command> [player]");
            register(registrar, new RepairCommand(), "Repair the item you are holding", "/<command>");
            register(registrar, new FlyCommand(), "Enable or disable flight for yourself or others", "/<command> [on|off] [player]");
            register(registrar, new RenameCommand(), "Rename an item's name", "/<command> <name>", List.of("itemname"));
            register(registrar, new CraftCommand(), "Open crafting table", "/<command>", List.of("workbench", "wb"));
            register(registrar, new TopCommand(), "Teleport to top-most block at current position", "/<command>");
            register(registrar, new AFKCommand(), "Mark yourself as AFK", "/<command>");
            register(registrar, new RtpCommand(), "Randomly teleport", "/<command>");
            register(registrar, new FarmShopCommand(), "Open farm shop", "/<command>");
            register(registrar, new GardensPlayersCommand(), "Gardens Players command", "/<command>");
            register(registrar, new WarpCommand(), "View warps", "/<command>", List.of("warps"));
            register(registrar, new WithdrawCommand(), "Withdraw acorns", "/<command>");
            register(registrar, new DepositCommand(), "Deposit acorns", "/<command>");
            register(registrar, new UpgradesCommand(), "Upgrade shop", "/<command>");
            register(registrar, new ShopCommand(), "View shop", "/<command>");
            register(registrar, new WardrobeCommand(), "View wardrobe", "/<command>");
            register(registrar, new RewardsCommand(), "View rewards", "/<command>", List.of("reward"));
            register(registrar, new SpawnerCommand(), "Change spawner", "/<command>", List.of("ss"));
            register(registrar, new TpposCommand(), "Teleport to a position", "/<command>");
            register(registrar, new MapCommand(), "View map link", "/<command>");
            register(registrar, new GamemodeCommand(), "Set gamemode", "/<command>", List.of("gm"));
            register(registrar, new TrashCommand(), "Open trash", "/<command>");
            register(registrar, new AnvilCommand(), "Open anvil", "/<command>");
            register(registrar, new LoomCommand(), "Open loom", "/<command>");
            register(registrar, new GrindstoneCommand(), "Open grindstone", "/<command>");
            register(registrar, new MoreCommand(), "Get more of held item", "/<command>");
            register(registrar, new MountCommand(), "Open mounts menu", "/<command>", List.of("mounts"));
            register(registrar, new QuestsCommand(), "View daily quests", "/<command>", List.of("quest", "q"));
            register(registrar, new QuestResetCommand(), "Reset the daily quests", "/<command>");
            register(registrar, new SeenCommand(), "See when a player was last online", "/<command> <player>");
            register(registrar, new TpOfflineCommand(), "Teleport to a player's last known location", "/<command> <player>");
            register(registrar, new LookupCommand(), "Find players by partial username", "/<command> <partial-username>");
            register(registrar, new DeathBackCommand(), "Teleport back to your most recent death location", "/<command>");
            register(registrar, new SmiteCommand(), "Strike lightning at the block you're looking at", "/<command>", List.of("lightning"));

            // Native Brigadier command tree (not bridged from a GardensCommand)

            // Commands that don't extend GardensCommand
            CommandBridge.register(registrar, new CurrencyCommand(GemsCurrencyHandler.INSTANCE), "gem", "View or modify gems", "/<command>", List.of("gems"));
            CommandBridge.register(registrar, new CurrencyCommand(AcornsCurrencyHandler.INSTANCE), "acorn", "View or modify acorns", "/<command>", List.of("acorns"));
            CommandBridge.register(registrar, new BalTopCommand(), "baltop", "View top balances", "/<command>", List.of());
            CommandBridge.register(registrar, new GenResourceCommand(), "genresource", "Start generation of resource worlds", "/<command>", List.of());
            CommandBridge.register(registrar, new ResetResourceCommand(), "resetresource", "Reset resource worlds", "/<command>", List.of());
        });
    }

    private static void register(io.papermc.paper.command.brigadier.Commands registrar, GardensCommand command, String description, String usage) {
        register(registrar, command, description, usage, List.of());
    }

    private static void register(io.papermc.paper.command.brigadier.Commands registrar, GardensCommand command, String description, String usage, List<String> aliases) {
        CommandBridge.register(registrar, command, command.getName(), description, usage, aliases);
    }
}
