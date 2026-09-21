package com.gardensmc.gardensplayers.command.registered;

import com.gardensmc.gardensplayers.guild.GuildsHandler;
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
 * Native Brigadier command tree for the guild system:
 * <pre>
 * /guild                       your guild's member menu, or the guilds list if you're not in one
 * /guild create &lt;name&gt;         names may contain spaces (max 13 chars)
 * /guild rename &lt;name&gt;         admin/owner — rename the guild
 * /guild description &lt;text|clear&gt; admin/owner — set or clear the description
 * /guild join &lt;guild&gt;          request to join a guild (or accept a standing invite)
 * /guild invite &lt;player&gt;       officer and above — invite a player to join
 * /guild uninvite &lt;player&gt;     officer and above — revoke an invite
 * /guild leave                 leave your guild (owner must disband instead)
 * /guild disband               owner only — delete the guild
 * /guild accept &lt;player&gt;       officer and above — approve a join request
 * /guild deny &lt;player|guild&gt;   officer and above deny a join request, or decline an invite you've received
 * /guild kick &lt;player&gt;         officer and above — remove a lower-ranked member
 * /guild promote|demote &lt;player&gt; admin/owner — move ranks (only the owner makes/removes admins)
 * /guild transfer &lt;player&gt;     owner — hand ownership to a member
 * /guild sethome               admin/owner — set the guild home here
 * /guild home                  travel to the guild home (any member)
 * /guild chat &lt;message&gt;        message all online guild members (also /guildchat, /gc)
 * /guild requests|info         list pending requests / show your guild
 *
 * Ranks (most senior first): owner &gt; admin &gt; officer &gt; member.
 * </pre>
 */
public final class GuildCommand {

    private static final String PERMISSION = "gplayers.guild";

    private GuildCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("guild")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::openGuild))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> stringArgAction(ctx, "name", GuildsHandler.INSTANCE::create))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> stringArgAction(ctx, "name", GuildsHandler.INSTANCE::rename))))
                .then(Commands.literal("description")
                        .then(Commands.literal("clear")
                                .executes(ctx -> selfAction(ctx, player -> GuildsHandler.INSTANCE.describe(player, null))))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> stringArgAction(ctx, "text", GuildsHandler.INSTANCE::describe))))
                .then(Commands.literal("join")
                        .then(Commands.argument("guild", StringArgumentType.greedyString())
                                .executes(ctx -> stringArgAction(ctx, "guild", GuildsHandler.INSTANCE::join))))
                .then(Commands.literal("leave").executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::leave)))
                .then(Commands.literal("disband").executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::disband)))
                .then(targetAction("invite", GuildsHandler.INSTANCE::invite))
                .then(targetAction("uninvite", GuildsHandler.INSTANCE::uninvite))
                .then(targetAction("accept", GuildsHandler.INSTANCE::accept))
                // deny takes a player (deny a join request) or a guild name (decline an invite), so it's greedy
                .then(Commands.literal("deny")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests(GuildCommand::suggestOnlinePlayers)
                                .executes(ctx -> stringArgAction(ctx, "target", GuildsHandler.INSTANCE::deny))))
                .then(targetAction("kick", GuildsHandler.INSTANCE::kick))
                .then(targetAction("promote", GuildsHandler.INSTANCE::promote))
                .then(targetAction("demote", GuildsHandler.INSTANCE::demote))
                .then(targetAction("transfer", GuildsHandler.INSTANCE::transfer))
                .then(Commands.literal("chat")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> stringArgAction(ctx, "message", GuildsHandler.INSTANCE::chat))))
                .then(Commands.literal("sethome").executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::setHome)))
                .then(Commands.literal("home").executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::home)))
                .then(Commands.literal("requests").executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::requests)))
                .then(Commands.literal("info").executes(ctx -> selfAction(ctx, GuildsHandler.INSTANCE::info)))
                .build();
    }

    /** A subcommand taking a single player name, suggested from online players (e.g. {@code /guild kick <player>}). */
    private static LiteralArgumentBuilder<CommandSourceStack> targetAction(String name, BiConsumer<Player, String> action) {
        return Commands.literal(name)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(GuildCommand::suggestOnlinePlayers)
                        .executes(ctx -> stringArgAction(ctx, "player", action)));
    }

    /** Invokes {@code action} with the sender and the named string argument (guild names may contain spaces). */
    private static int stringArgAction(CommandContext<CommandSourceStack> ctx, String argName, BiConsumer<Player, String> action) {
        if (ctx.getSource().getSender() instanceof Player player) {
            action.accept(player, StringArgumentType.getString(ctx, argName));
        } else {
            sendPlayerOnly(ctx);
        }
        return Command.SINGLE_SUCCESS;
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
