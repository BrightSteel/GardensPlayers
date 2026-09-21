package com.gardensmc.gardensplayers.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges the plugin's legacy {@link CommandExecutor}/{@link TabCompleter} command implementations onto Paper's
 * Brigadier command system, so commands no longer need to be declared in {@code plugin.yml}.
 * <p>
 * Each command is registered as a single literal with an optional greedy-string argument that is split back into the
 * {@code String[] args} the existing {@code onCommand}/{@code onTabComplete} methods expect, preserving their behaviour.
 */
public final class CommandBridge {

    private CommandBridge() {
    }

    /**
     * Registers a command onto the supplied Brigadier registrar.
     *
     * @param registrar   the registrar obtained from the {@code COMMANDS} lifecycle event
     * @param executor    the legacy executor (also used as the tab completer if it implements {@link TabCompleter})
     * @param name        the primary command label
     * @param description the help description, or {@code null}
     * @param usage       the usage message exposed via {@link org.bukkit.command.Command#getUsage()}
     * @param aliases     additional labels for the command
     */
    public static void register(
            Commands registrar,
            CommandExecutor executor,
            String name,
            String description,
            String usage,
            List<String> aliases
    ) {
        // A lightweight Bukkit Command so executors that read command.getName()/getUsage() keep working.
        var bukkitCommand = new org.bukkit.command.Command(name, description == null ? "" : description, usage, aliases) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return true;
            }
        };

        LiteralCommandNode<CommandSourceStack> node = Commands.literal(name)
                .executes(ctx -> run(executor, bukkitCommand, name, ctx, ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggest(executor, bukkitCommand, name, ctx, builder))
                        .executes(ctx -> run(executor, bukkitCommand, name, ctx, StringArgumentType.getString(ctx, "args"))))
                .build();

        registrar.register(node, description, aliases);
    }

    private static int run(
            CommandExecutor executor,
            org.bukkit.command.Command bukkitCommand,
            String name,
            CommandContext<CommandSourceStack> ctx,
            String raw
    ) {
        CommandSender sender = ctx.getSource().getSender();
        String[] args = raw.isBlank() ? new String[0] : raw.trim().split(" +");
        executor.onCommand(sender, bukkitCommand, name, args);
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggest(
            CommandExecutor executor,
            org.bukkit.command.Command bukkitCommand,
            String name,
            CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder
    ) {
        if (!(executor instanceof TabCompleter completer)) {
            return builder.buildFuture();
        }
        // Reproduce the legacy String[] args the completer expects: the last element is the (possibly empty) token
        // currently being typed.
        String remaining = builder.getRemaining();
        String[] args = remaining.split(" ", -1);
        String lastToken = args[args.length - 1];
        List<String> suggestions = completer.onTabComplete(ctx.getSource().getSender(), bukkitCommand, name, args);
        if (suggestions == null) {
            return builder.buildFuture();
        }
        // Offset the builder so suggestions replace only the token being typed, matching Bukkit's prefix completion.
        SuggestionsBuilder offset = builder.createOffset(builder.getStart() + remaining.length() - lastToken.length());
        suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase().startsWith(lastToken.toLowerCase()))
                .forEach(offset::suggest);
        return offset.buildFuture();
    }
}
