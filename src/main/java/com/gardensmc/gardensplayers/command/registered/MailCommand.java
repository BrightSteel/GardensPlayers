package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.mail.MailHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /mail [page]} reads a page of the sender's inbox, {@code /mail send <user> <message>} sends mail, and
 * {@code /mail clear} deletes all of the sender's mail.
 */
public final class MailCommand {

    private static final String PERMISSION = "gplayers.mail";

    private MailCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("mail")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(ctx -> read(ctx, 1))
                .then(Commands.literal("read")
                        .executes(ctx -> read(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> read(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(Commands.literal("clear").executes(MailCommand::clear))
                .then(Commands.literal("send")
                        .then(Commands.argument("user", StringArgumentType.word())
                                .suggests(MailCommand::suggestOnlinePlayers)
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(MailCommand::send))))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> read(ctx, IntegerArgumentType.getInteger(ctx, "page"))))
                .build();
    }

    private static int read(CommandContext<CommandSourceStack> ctx, int page) {
        if (ctx.getSource().getSender() instanceof Player player) {
            MailHandler.INSTANCE.read(player, page);
        } else {
            sendPlayerOnly(ctx);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int send(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            MailHandler.INSTANCE.send(player,
                    StringArgumentType.getString(ctx, "user"),
                    StringArgumentType.getString(ctx, "message"));
        } else {
            sendPlayerOnly(ctx);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            MailHandler.INSTANCE.clear(player);
        } else {
            sendPlayerOnly(ctx);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase();
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static void sendPlayerOnly(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command"));
    }
}
