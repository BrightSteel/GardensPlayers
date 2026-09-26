package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.exception.InvalidStateException;
import com.gardensmc.gardensplayers.command.type.PlayerExecutorCommand;
import com.gardensmc.gardensplayers.currency.AcornsCurrencyHandler;
import com.gardensmc.gardensplayers.death.DeathLocationTracker;
import com.gardensmc.gardensplayers.menu.BedrockDeathBackMenu;
import com.gardensmc.gardensplayers.menu.DeathBackConfirmMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * /deathback — returns the player to their most recent death location for the configured acorn cost.
 * Triggered by clicking the message sent on death (see {@link com.gardensmc.gardensplayers.listener.DeathListener}).
 * <p>
 * Running it with no argument shows a Yes/No confirmation first — a Cumulus modal form for Bedrock players,
 * an inventory confirm menu for Java players. Both confirm by re-running {@code /deathback confirm}, which is
 * where the charge + teleport actually happens. Shares the {@code gplayers.deathback} permission node with the
 * death message.
 */
public class DeathBackCommand extends PlayerExecutorCommand {

    public DeathBackCommand() {
        super("deathback");
    }

    @Override
    public void execute(Player executor, String[] args) {
        super.execute(executor, args);

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            sendMessage(executor, "<gray>Death teleport cancelled");
            return;
        }

        // Validate up front so we never show a confirmation we can't honour.
        Location location = getValidDeathLocation(executor);
        int cost = GardensPlayers.config.getDeathBackCost();
        var acorns = AcornsCurrencyHandler.INSTANCE;
        if (!acorns.has(executor.getUniqueId(), cost)) {
            throw new InvalidStateException("You need %d acorns to teleport to your death location".formatted(cost));
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            // pay first, teleport only once the acorns were actually taken (a free deathback skips paying)
            var paid = cost > 0 ? acorns.takeCurrency(executor.getUniqueId(), cost) : CompletableFuture.completedFuture(true);
            paid.thenAccept(taken -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                if (!taken) {
                    sendMessage(executor, "<red>You need %d acorns to teleport to your death location".formatted(cost));
                    return;
                }
                executor.teleport(location);
                DeathLocationTracker.INSTANCE.clear(executor.getUniqueId());
                sendMessage(executor, "<green>Teleported to your death location for %d acorns".formatted(cost));
            }));
            return;
        }

        // No sub-action: ask for confirmation first.
        if (FloodgateApi.getInstance().isFloodgatePlayer(executor.getUniqueId())) {
            BedrockDeathBackMenu.open(executor, cost);
        } else {
            DeathBackConfirmMenu.open(executor, cost);
        }
    }

    private Location getValidDeathLocation(Player executor) {
        var location = DeathLocationTracker.INSTANCE.get(executor.getUniqueId());
        if (location == null) {
            throw new InvalidStateException("You have no recent death location to return to");
        }
        if (!location.isWorldLoaded()) { // getWorld() throws for a world that was unloaded (e.g. a reset resource world)
            throw new InvalidStateException("Your death location is in a world that is no longer loaded");
        }
        return location;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Sub-actions are internal (driven by the confirmation UI), so nothing to suggest.
        return List.of();
    }
}
