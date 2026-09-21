package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.guild.GuildsHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * {@code /guildchat <message>} (aliases {@code /gc}) sends a message to every online member of the sender's guild. This
 * is the same action as {@code /guild chat}, exposed as a top-level command for convenience.
 */
public final class GuildChatCommand {

    private static final String PERMISSION = "gplayers.guild";

    private GuildChatCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("guildchat")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(GuildChatCommand::usage)
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(GuildChatCommand::chat))
                .build();
    }

    private static int chat(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            GuildsHandler.INSTANCE.chat(player, StringArgumentType.getString(ctx, "message"));
        } else {
            ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int usage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /guildchat <message>"));
        return Command.SINGLE_SUCCESS;
    }
}
