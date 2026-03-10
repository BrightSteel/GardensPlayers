package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.currency.Currency;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

@AllArgsConstructor
public class CurrencyCommand implements TabExecutor {

    private final Currency currency;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            if (commandSender instanceof Player player) {
                var amount = currency.getCurrency(player);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>You have " + amount + " " + currency.symbol()));
            } else {
                commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Provide an argument"));
            }
        } else {
            try {
                switch (args[0].toLowerCase()) {
                    case "give" -> giveCurrency(commandSender, args);
                    case "set" -> setCurrency(commandSender, args);
                    case "get" -> getCurrency(commandSender, args);
                }
            } catch (NumberFormatException e) {
                commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Amount must be a number!"));
            }
        }

        return true;
    }

    private void sendNoPerms(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to run this command"));
    }

    private void giveCurrency(CommandSender sender, String[] args) throws NumberFormatException {
        if (!sender.hasPermission(currency.name() + ".give")) {
            sendNoPerms(sender);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Provide <player> and <amount> args"));
            return;
        }
        var target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("<red>Couldn't find player " + args[1]);
            return;
        }
        var amount = Integer.parseInt(args[2]);
        if (amount <= 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Amount must be greater than 0!"));
            return;
        }
        currency.addCurrency(target, amount).thenRun(() -> {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Gave %s %s %s".formatted(target.getName(), amount, currency.symbol())));
            target.sendMessage(MiniMessage.miniMessage().deserialize("<green>%s gave you %s %s".formatted(sender.getName(), amount, currency.symbol())));
        });
    }

    private void setCurrency(CommandSender sender, String[] args) throws NumberFormatException {
        if (!sender.hasPermission(currency.name() + ".set")) {
            sendNoPerms(sender);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Provide <player> and <amount> args"));
            return;
        }
        var target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("<red>Couldn't find player " + args[1]);
            return;
        }
        var amount = Integer.parseInt(args[2]);
        if (amount < 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Amount must be positive!"));
            return;
        }
        currency.setCurrency(target, amount).thenRun(() -> {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set %s's %s to %s".formatted(target.getName(), currency.name(), amount)));
            target.sendMessage(MiniMessage.miniMessage().deserialize("<green>%s set your %s to %s".formatted(sender.getName(), currency.name(), amount)));
        });
    }

    private void getCurrency(CommandSender sender, String[] args) throws NumberFormatException {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Console cannot get" + currency.name()));
            return;
        }
        if (!sender.hasPermission(currency.name() + ".get")) {
            sendNoPerms(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Provide amount arg"));
            return;
        }
        var amount = Integer.parseInt(args[1]);
        if (amount <= 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Amount must be greater than 0!"));
            return;
        }
        currency.addCurrency(player, amount).thenRun(() -> sender.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>Received %s %s".formatted(amount, currency.symbol())))
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return Stream.of("give", "set", "get").filter(arg -> commandSender.hasPermission(currency.name() + "." + arg)).toList();
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("get")) {
                return List.of("amount");
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set")) {
                return List.of("amount");
            }
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
