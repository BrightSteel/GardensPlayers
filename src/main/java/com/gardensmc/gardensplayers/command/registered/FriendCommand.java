package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.friends.FriendsHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Native Brigadier command tree for the friends system:
 * <pre>
 * /friend add|accept|deny|remove &lt;player&gt;
 * /friend list|requests
 * </pre>
 */
public final class FriendCommand {

    private static final String PERMISSION = "gplayers.friend";

    private FriendCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("friend")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(ctx -> selfAction(ctx, FriendsHandler.INSTANCE::openMenu))
                .then(playerAction("add", FriendsHandler.INSTANCE::add))
                .then(playerAction("deny", FriendsHandler.INSTANCE::deny))
                .then(playerAction("remove", FriendsHandler.INSTANCE::remove))
                .then(Commands.literal("list").executes(ctx -> selfAction(ctx, FriendsHandler.INSTANCE::openMenu)))
                .then(Commands.literal("requests").executes(ctx -> selfAction(ctx, FriendsHandler.INSTANCE::requests)))
                .build();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> playerAction(String name, BiConsumer<Player, String> action) {
        return Commands.literal(name)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(FriendCommand::suggestOnlinePlayers)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                sendPlayerOnly(ctx);
                                return Command.SINGLE_SUCCESS;
                            }
                            action.accept(player, StringArgumentType.getString(ctx, "player"));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static int selfAction(CommandContext<CommandSourceStack> ctx, Consumer<Player> action) {
        if (ctx.getSource().getSender() instanceof Player player) {
            action.accept(player);
        } else {
            sendPlayerOnly(ctx);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase();
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(playerName -> playerName.toLowerCase().startsWith(prefix))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static void sendPlayerOnly(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command"));
    }
}
