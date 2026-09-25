package com.gardensmc.gardensplayers.guild;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.GuildCache;
import com.gardensmc.gardensplayers.cache.PlayerGuildCache;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.GuildEntry;
import com.gardensmc.gardensplayers.database.entry.GuildJoinRequestEntry;
import com.gardensmc.gardensplayers.database.entry.GuildMemberEntry;
import com.gardensmc.gardensplayers.database.entry.GuildOwnerEntry;
import com.gardensmc.gardensplayers.database.entry.nested.LocationJsonEntry;
import com.gardensmc.gardensplayers.menu.GuildMembersTileMenu;
import com.gardensmc.gardensplayers.menu.GuildsMenu;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import com.gardensmc.gardensplayers.menu.tile.GuildJoinRequestTileItem;
import com.gardensmc.gardensplayers.menu.tile.GuildMemberTileItem;
import com.gardensmc.gardensplayers.menu.tile.TileCategory;
import com.gardensmc.gardensplayers.util.FontShift;
import com.gardensmc.gardensplayers.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Business logic for the guild command suite: every
 * action runs its blocking database work off the main thread, then returns a {@link Runnable} that performs all
 * messaging (and online-player lookups) back on the main thread.
 *
 * <p>Permission model: a guild's {@code OWNER} can do everything; an {@code OFFICER} (the "manager" rank) can approve or
 * deny join requests and kick ordinary members; everyone else is a {@code MEMBER}. Promoting/demoting officers and
 * disbanding are owner-only, and the owner cannot leave (they must disband).
 */
public class GuildsHandler {

    public static final GuildsHandler INSTANCE = new GuildsHandler();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    /**
     * MiniMessage restricted to visual tags (colours, decorations, gradients) for player-authored guild chat — no
     * click, hover, insertion, font or newline tags, so players can't embed links or other interactive content.
     */
    private static final MiniMessage CHAT_MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.reset()))
            .build();
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_ ,$&'.-]{3,13}$");
    private static final int MAX_DESCRIPTION_LENGTH = 50;
    private static final String INVALID_NAME = "Guild names must be 3-13 characters: letters, numbers, spaces and , $ & ' . - _ only";
    private static final String MANAGE_MEMBERS_DENIED = "Only officers and above can manage members";
    private static final String MANAGE_GUILD_DENIED = "Only admins and the owner can do that";
    private static final String MANAGE_RANKS_DENIED = "Only admins and the owner can manage ranks";
    /** Permission required to use MiniMessage formatting in guild chat; without it, messages are sent as plain text. */
    private static final String CHAT_FORMAT_PERMISSION = "gplayers.guild.chat.format";

    private GuildsHandler() {
    }

    /**
     * Opens the paginated guilds menu, resolving the guild list (and whether the viewer is already in a guild, which
     * gates the right-click-to-join action) off the main thread before building the GUI.
     */
    public void openMenu(Player sender) {
        CompletableFuture
                .supplyAsync(() -> new MenuData(
                        Tables.guildsTable.getGuildsWithOwners(),
                        Tables.guildMembersTable.getMembership(sender.getUniqueId()) != null))
                .thenAccept(data -> Bukkit.getScheduler().runTask(
                        GardensPlayers.plugin,
                        () -> new GuildsMenu(sender, data.guilds(), data.inGuild()).open()))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to open guilds menu", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> error(sender, "An internal error occurred"));
                    return null;
                });
    }

    private record MenuData(List<GuildOwnerEntry> guilds, boolean inGuild) {
    }

    /**
     * Entry point for {@code /guild}: if the sender is in a guild, opens a tile menu of its members; otherwise falls back
     * to the paginated list of all guilds. Member data is resolved from the caches off the main thread.
     */
    public void openGuild(Player sender) {
        UUID self = sender.getUniqueId();
        PlayerGuildCache.INSTANCE.get(self)
                .thenAccept(guildId -> {
                    if (guildId.isEmpty()) {
                        // not in a guild — browse all guilds instead
                        openMenu(sender);
                        return;
                    }
                    GuildCache.CachedGuild cached = GuildCache.INSTANCE.getSync(guildId.get());
                    CategoryPages pages = buildGuildCategoryPages(self, guildId.get(), cached);
                    String guildName = cached.guild() != null ? cached.guild().getName() : "Guild";
                    // centre the name within the 80px title section that starts 35px left of the cursor
                    int sectionStart = 35;
                    int sectionWidth = 80;
                    int nameWidth = FontShift.INSTANCE.getStringWidth(guildName);
                    int leftPad = (sectionWidth - nameWidth) / 2;
                    String titleChars =
                            FontShift.INSTANCE.getShift(leftPad - sectionStart)
                            + "<font:gardens:tile_upper>" + guildName + "</font>"
                            + FontShift.INSTANCE.getShift(sectionStart - leftPad - nameWidth);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin,
                            () -> new GuildMembersTileMenu(cached, sender, pages, '\u015D', titleChars).open());
                })
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to open guild menu", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> error(sender, "An internal error occurred"));
                    return null;
                });
    }

    /** Reloads the guild tile menu in place after an action (e.g. accepting a join request), preserving the open tab. */
    public void refreshGuildMenu(Player viewer, TileMenu menu) {
        UUID self = viewer.getUniqueId();
        PlayerGuildCache.INSTANCE.get(self)
                .thenAccept(guildId -> {
                    if (guildId.isEmpty()) {
                        return;
                    }
                    GuildCache.CachedGuild cached = GuildCache.INSTANCE.getSync(guildId.get());
                    CategoryPages pages = buildGuildCategoryPages(self, guildId.get(), cached);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> menu.reopenWith(pages));
                })
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to refresh guild menu", throwable);
                    return null;
                });
    }

    /**
     * Builds the guild tile menu's categories for {@code self} from already-resolved guild data: the member roster, plus
     * a join-requests tab when the viewer is an officer or above (only they can resolve requests). Must be called off the
     * main thread (it queries pending requests).
     */
    private CategoryPages buildGuildCategoryPages(UUID self, int guildId, GuildCache.CachedGuild cached) {
        GuildRole viewerRole = cached.members().stream()
                .filter(member -> member.getMemberUUID().equals(self))
                .map(GuildMemberEntry::getRole)
                .findFirst()
                .orElse(null);

        var categories = new ArrayList<TileCategory>();
        categories.add(new TileCategory("members", "Guild Members", "icon_profile",
                cached.members().stream().map(member -> new GuildMemberTileItem(member, viewerRole)).toList()));

        if (viewerRole != null && viewerRole.isAtLeast(GuildRole.OFFICER)) {
            var requests = Tables.guildJoinRequestsTable.getRequestsForGuild(guildId);
            categories.add(new TileCategory("requests", "Join Requests", "icon_page_left",
                    requests.stream().map(GuildJoinRequestTileItem::new).toList()));
        }
        return new CategoryPages(categories);
    }

    public void create(Player sender, String name) {
        // names may contain spaces; the command joins the raw args, so trim the surrounding whitespace
        String guildName = name.trim();
        runAsync(sender, () -> {
            if (!NAME_PATTERN.matcher(guildName).matches()) {
                return () -> error(sender, INVALID_NAME);
            }
            if (Tables.guildMembersTable.getMembership(sender.getUniqueId()) != null) {
                return () -> error(sender, "You're already in a guild — leave it first with /guild leave");
            }
            if (Tables.guildsTable.getGuildByName(guildName) != null) {
                return () -> error(sender, "A guild named '" + guildName + "' already exists");
            }
            Tables.guildsTable.createGuild(guildName, null);
            GuildEntry guild = Tables.guildsTable.getGuildByName(guildName);
            Tables.guildMembersTable.addMember(sender.getUniqueId(), guild.getId(), GuildRole.OWNER);
            // joining a guild clears any outstanding requests and invites this player had
            Tables.guildJoinRequestsTable.removeAllRequestsBy(sender.getUniqueId());
            Tables.guildInvitesTable.removeAllInvitesFor(sender.getUniqueId());
            PlayerGuildCache.INSTANCE.remove(sender.getUniqueId());
            invalidateGuild(guild.getId());
            return () -> message(sender, "Created guild <yellow>" + guildName + "</yellow> — you are its owner");
        });
    }

    public void disband(Player sender) {
        runAsync(sender, () -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(sender.getUniqueId());
            if (membership == null) {
                return () -> error(sender, "You're not in a guild");
            }
            if (membership.getRole() != GuildRole.OWNER) {
                return () -> error(sender, "Only the guild owner can disband the guild");
            }
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            List<GuildMemberEntry> members = Tables.guildMembersTable.getMembers(membership.getGuildId());
            // deleting the guild cascades away its members and pending join requests
            Tables.guildsTable.deleteGuild(membership.getGuildId());
            invalidateGuild(membership.getGuildId());
            members.forEach(member -> PlayerGuildCache.INSTANCE.remove(member.getMemberUUID()));
            String guildName = guild != null ? guild.getName() : "your guild";
            return () -> {
                message(sender, "Disbanded <yellow>" + guildName + "</yellow>");
                for (GuildMemberEntry member : members) {
                    if (!member.getMemberUUID().equals(sender.getUniqueId())) {
                        notifyIfOnline(member.getMemberUUID(), guildName + " has been disbanded by its owner");
                    }
                }
            };
        });
    }

    public void leave(Player sender) {
        runAsync(sender, () -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(sender.getUniqueId());
            if (membership == null) {
                return () -> error(sender, "You're not in a guild");
            }
            if (membership.getRole() == GuildRole.OWNER) {
                return () -> error(sender, "As the owner you can't leave — disband the guild with /guild disband instead");
            }
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            // capture members before removal so we can notify the remaining managers
            List<GuildMemberEntry> members = Tables.guildMembersTable.getMembers(membership.getGuildId());
            Tables.guildMembersTable.removeMember(sender.getUniqueId());
            PlayerGuildCache.INSTANCE.remove(sender.getUniqueId());
            invalidateGuild(membership.getGuildId());
            List<GuildMemberEntry> managers = members.stream()
                    .filter(member -> member.getRole().isAtLeast(GuildRole.OFFICER))
                    .filter(member -> !member.getMemberUUID().equals(sender.getUniqueId()))
                    .toList();
            String guildName = guild != null ? guild.getName() : "your guild";
            return () -> {
                message(sender, "You left <yellow>" + guildName + "</yellow>");
                for (GuildMemberEntry manager : managers) {
                    notifyIfOnline(manager.getMemberUUID(), sender.getName() + " left " + guildName);
                }
            };
        });
    }

    public void join(Player sender, String name) {
        String guildName = name.trim();
        runAsync(sender, () -> {
            if (Tables.guildMembersTable.getMembership(sender.getUniqueId()) != null) {
                return () -> error(sender, "You're already in a guild — leave it first with /guild leave");
            }
            GuildEntry guild = Tables.guildsTable.getGuildByName(guildName);
            if (guild == null) {
                return () -> error(sender, "There's no guild named '" + guildName + "'");
            }
            // an outstanding invite is a pre-approval — accept it and join immediately
            if (Tables.guildInvitesTable.getInvite(guild.getId(), sender.getUniqueId()) != null) {
                joinGuild(sender.getUniqueId(), guild.getId());
                List<GuildMemberEntry> managers = managers(guild.getId());
                return () -> {
                    message(sender, "You joined <yellow>" + guild.getName() + "</yellow>");
                    for (GuildMemberEntry manager : managers) {
                        notifyIfOnline(manager.getMemberUUID(), sender.getName() + " joined " + guild.getName());
                    }
                };
            }
            if (Tables.guildJoinRequestsTable.getRequest(guild.getId(), sender.getUniqueId()) != null) {
                return () -> error(sender, "You already have a pending request to join " + guild.getName());
            }
            Tables.guildJoinRequestsTable.addRequest(guild.getId(), sender.getUniqueId());
            List<GuildMemberEntry> managers = managers(guild.getId());
            return () -> {
                message(sender, "Requested to join <yellow>" + guild.getName() + "</yellow> — an officer or above must approve");
                for (GuildMemberEntry manager : managers) {
                    notifyIfOnline(manager.getMemberUUID(),
                            sender.getName() + " wants to join " + guild.getName() + " — /guild accept " + sender.getName());
                }
            };
        });
    }

    public void invite(Player sender, String targetName) {
        roleAction(sender, GuildRole.OFFICER, MANAGE_MEMBERS_DENIED, membership -> {
            UUID targetUUID = Tables.playerTable.getUUID(targetName);
            if (targetUUID == null) {
                return () -> error(sender, "Player '" + targetName + "' has never joined the server");
            }
            if (targetUUID.equals(sender.getUniqueId())) {
                return () -> error(sender, "You can't invite yourself");
            }
            if (Tables.guildMembersTable.getMembership(targetUUID) != null) {
                return () -> error(sender, targetName + " is already in a guild");
            }
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            String guildName = guild != null ? guild.getName() : "the guild";
            // if they already asked to join, the invite resolves their request and admits them straight away
            if (Tables.guildJoinRequestsTable.getRequest(membership.getGuildId(), targetUUID) != null) {
                joinGuild(targetUUID, membership.getGuildId());
                return () -> {
                    message(sender, "Accepted " + targetName + "'s pending request — they've joined the guild");
                    notifyIfOnline(targetUUID, "Your request to join " + guildName + " was accepted");
                };
            }
            if (Tables.guildInvitesTable.getInvite(membership.getGuildId(), targetUUID) != null) {
                return () -> error(sender, targetName + " already has a pending invite");
            }
            Tables.guildInvitesTable.addInvite(membership.getGuildId(), targetUUID);
            return () -> {
                message(sender, "Invited " + targetName + " to the guild");
                notifyIfOnline(targetUUID,
                        "You've been invited to join " + guildName + " — use /guild join " + guildName + " to accept");
            };
        });
    }

    /** Revokes an invite the guild previously sent to a player. Officer-side counterpart to declining via {@link #deny}. */
    public void uninvite(Player sender, String targetName) {
        roleAction(sender, GuildRole.OFFICER, MANAGE_MEMBERS_DENIED, membership -> {
            UUID targetUUID = Tables.playerTable.getUUID(targetName);
            if (targetUUID == null) {
                return () -> error(sender, "Player '" + targetName + "' has never joined the server");
            }
            if (Tables.guildInvitesTable.getInvite(membership.getGuildId(), targetUUID) == null) {
                return () -> error(sender, targetName + " doesn't have a pending invite");
            }
            Tables.guildInvitesTable.removeInvite(membership.getGuildId(), targetUUID);
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            String guildName = guild != null ? guild.getName() : "a guild";
            return () -> {
                message(sender, "Revoked " + targetName + "'s invite");
                notifyIfOnline(targetUUID, "Your invite to " + guildName + " was revoked");
            };
        });
    }

    /** Adds {@code playerUUID} to {@code guildId} as a member, clearing their pending requests/invites and caches. */
    private void joinGuild(UUID playerUUID, int guildId) {
        Tables.guildMembersTable.addMember(playerUUID, guildId, GuildRole.MEMBER);
        Tables.guildJoinRequestsTable.removeAllRequestsBy(playerUUID);
        Tables.guildInvitesTable.removeAllInvitesFor(playerUUID);
        PlayerGuildCache.INSTANCE.remove(playerUUID);
        invalidateGuild(guildId);
    }

    public void accept(Player sender, String targetName) {
        accept(sender, targetName, null);
    }

    public void accept(Player sender, String targetName, @Nullable Runnable onComplete) {
        roleAction(sender, GuildRole.OFFICER, MANAGE_MEMBERS_DENIED, membership -> {
            GuildJoinRequestEntry request = findRequest(membership.getGuildId(), targetName);
            if (request == null) {
                return () -> error(sender, "No pending request from '" + targetName + "' to join your guild");
            }
            UUID requester = request.getRequesterUUID();
            if (Tables.guildMembersTable.getMembership(requester) != null) {
                Tables.guildJoinRequestsTable.removeRequest(membership.getGuildId(), requester);
                return () -> error(sender, request.getRequesterUsername() + " has already joined another guild");
            }
            joinGuild(requester, membership.getGuildId());
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            String guildName = guild != null ? guild.getName() : "the guild";
            return () -> {
                message(sender, "Accepted " + request.getRequesterUsername() + " into the guild");
                notifyIfOnline(requester, "Your request to join " + guildName + " was accepted");
            };
        }, onComplete);
    }

    /**
     * Dual-purpose denial: if the sender is in a guild they deny a join request from the named <em>player</em> (officer
     * and above); otherwise they decline an invite from the named <em>guild</em>. The two never overlap, since a player
     * already in a guild can't hold invites.
     */
    public void deny(Player sender, String arg) {
        deny(sender, arg, null);
    }

    public void deny(Player sender, String arg, @Nullable Runnable onComplete) {
        runAsync(sender, () -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(sender.getUniqueId());
            if (membership != null) {
                // in a guild: officer-side denial of a join request from the named player
                if (!membership.getRole().isAtLeast(GuildRole.OFFICER)) {
                    return () -> error(sender, MANAGE_MEMBERS_DENIED);
                }
                GuildJoinRequestEntry request = findRequest(membership.getGuildId(), arg);
                if (request == null) {
                    return () -> error(sender, "No pending request from '" + arg + "' to join your guild");
                }
                Tables.guildJoinRequestsTable.removeRequest(membership.getGuildId(), request.getRequesterUUID());
                GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
                String guildName = guild != null ? guild.getName() : "the guild";
                return () -> {
                    message(sender, "Denied " + request.getRequesterUsername() + "'s request to join");
                    notifyIfOnline(request.getRequesterUUID(), "Your request to join " + guildName + " was denied");
                };
            }
            // not in a guild: decline an invite from the named guild
            String guildName = arg.trim();
            GuildEntry guild = Tables.guildsTable.getGuildByName(guildName);
            if (guild == null) {
                return () -> error(sender, "There's no guild named '" + guildName + "'");
            }
            if (Tables.guildInvitesTable.getInvite(guild.getId(), sender.getUniqueId()) == null) {
                return () -> error(sender, "You don't have an invite from " + guild.getName());
            }
            Tables.guildInvitesTable.removeInvite(guild.getId(), sender.getUniqueId());
            List<GuildMemberEntry> managers = managers(guild.getId());
            return () -> {
                message(sender, "Declined the invite from <yellow>" + guild.getName() + "</yellow>");
                for (GuildMemberEntry manager : managers) {
                    notifyIfOnline(manager.getMemberUUID(), sender.getName() + " declined your guild's invite");
                }
            };
        }, onComplete);
    }

    public void kick(Player sender, String targetName) {
        roleAction(sender, GuildRole.OFFICER, MANAGE_MEMBERS_DENIED, membership -> {
            GuildMemberEntry target = findMember(membership.getGuildId(), targetName);
            if (target == null) {
                return () -> error(sender, "'" + targetName + "' isn't in your guild");
            }
            if (target.getMemberUUID().equals(sender.getUniqueId())) {
                return () -> error(sender, "You can't kick yourself — use /guild leave");
            }
            // you can only kick members ranked strictly below you
            if (!membership.getRole().isHigherThan(target.getRole())) {
                return () -> error(sender, "You can only kick members ranked below you");
            }
            Tables.guildMembersTable.removeMember(target.getMemberUUID());
            PlayerGuildCache.INSTANCE.remove(target.getMemberUUID());
            invalidateGuild(membership.getGuildId());
            return () -> {
                message(sender, "Kicked " + target.getMemberUsername() + " from the guild");
                notifyIfOnline(target.getMemberUUID(), "You were kicked from the guild by " + sender.getName());
            };
        });
    }

    public void promote(Player sender, String targetName) {
        roleAction(sender, GuildRole.ADMIN, MANAGE_RANKS_DENIED, membership -> {
            GuildMemberEntry target = findMember(membership.getGuildId(), targetName);
            if (target == null || target.getMemberUUID().equals(sender.getUniqueId())) {
                return () -> error(sender, "'" + targetName + "' isn't a member of your guild");
            }
            GuildRole next = target.getRole().promoted();
            if (next == null) {
                return () -> error(sender, target.getMemberUsername() + " is already at the highest rank — use /guild transfer to hand over ownership");
            }
            // you must outrank the rank you're granting, so only the owner can create admins
            if (!membership.getRole().isHigherThan(next)) {
                return () -> error(sender, "Only the owner can promote a member to " + next.name().toLowerCase());
            }
            Tables.guildMembersTable.setRole(target.getMemberUUID(), next);
            invalidateGuild(membership.getGuildId());
            String rankName = next.name().toLowerCase();
            return () -> {
                message(sender, "Promoted " + target.getMemberUsername() + " to " + rankName);
                notifyIfOnline(target.getMemberUUID(), "You were promoted to " + rankName + " by " + sender.getName());
            };
        });
    }

    public void demote(Player sender, String targetName) {
        roleAction(sender, GuildRole.ADMIN, MANAGE_RANKS_DENIED, membership -> {
            GuildMemberEntry target = findMember(membership.getGuildId(), targetName);
            if (target == null) {
                return () -> error(sender, "'" + targetName + "' isn't a member of your guild");
            }
            GuildRole previous = target.getRole().demoted();
            if (previous == null) {
                return () -> error(sender, target.getMemberUsername() + " is already at the lowest rank");
            }
            // you must outrank the member being demoted, so only the owner can demote an admin
            if (!membership.getRole().isHigherThan(target.getRole())) {
                return () -> error(sender, "Only the owner can demote " + target.getMemberUsername());
            }
            Tables.guildMembersTable.setRole(target.getMemberUUID(), previous);
            invalidateGuild(membership.getGuildId());
            String rankName = previous.name().toLowerCase();
            return () -> {
                message(sender, "Demoted " + target.getMemberUsername() + " to " + rankName);
                notifyIfOnline(target.getMemberUUID(), "You were demoted to " + rankName + " by " + sender.getName());
            };
        });
    }

    /** Renames the guild. Admin or owner. */
    public void rename(Player sender, String name) {
        String guildName = name.trim();
        roleAction(sender, GuildRole.ADMIN, MANAGE_GUILD_DENIED, membership -> {
            if (!NAME_PATTERN.matcher(guildName).matches()) {
                return () -> error(sender, INVALID_NAME);
            }
            GuildEntry existing = Tables.guildsTable.getGuildByName(guildName);
            if (existing != null && existing.getId() != membership.getGuildId()) {
                return () -> error(sender, "A guild named '" + guildName + "' already exists");
            }
            Tables.guildsTable.rename(membership.getGuildId(), guildName);
            invalidateGuild(membership.getGuildId());
            return () -> message(sender, "Renamed your guild to <yellow>" + guildName + "</yellow>");
        });
    }

    /** Sets (or, when {@code description} is {@code null}, clears) the guild's description. Admin or owner. */
    public void describe(Player sender, @Nullable String description) {
        String trimmed = description == null ? null : description.trim();
        roleAction(sender, GuildRole.ADMIN, MANAGE_GUILD_DENIED, membership -> {
            if (trimmed != null && trimmed.length() > MAX_DESCRIPTION_LENGTH) {
                return () -> error(sender, "Guild descriptions can be at most " + MAX_DESCRIPTION_LENGTH + " characters");
            }
            Tables.guildsTable.setDescription(membership.getGuildId(), trimmed);
            invalidateGuild(membership.getGuildId());
            return () -> message(sender, trimmed == null
                    ? "Cleared your guild's description"
                    : "Updated your guild's description");
        });
    }

    public void transfer(Player sender, String targetName) {
        ownerAction(sender, "transfer ownership", membership -> {
            GuildMemberEntry target = findMember(membership.getGuildId(), targetName);
            if (target == null) {
                return () -> error(sender, "'" + targetName + "' isn't a member of your guild");
            }
            if (target.getMemberUUID().equals(sender.getUniqueId())) {
                return () -> error(sender, "You're already the owner");
            }
            // promote the target to owner, then step the previous owner down to admin (one owner at a time)
            Tables.guildMembersTable.setRole(target.getMemberUUID(), GuildRole.OWNER);
            Tables.guildMembersTable.setRole(sender.getUniqueId(), GuildRole.ADMIN);
            invalidateGuild(membership.getGuildId());
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            String guildName = guild != null ? guild.getName() : "the guild";
            return () -> {
                message(sender, "Transferred ownership of <yellow>" + guildName + "</yellow> to "
                        + target.getMemberUsername() + " — you are now an admin");
                notifyIfOnline(target.getMemberUUID(), "You are now the owner of " + guildName);
            };
        });
    }

    public void requests(Player sender) {
        roleAction(sender, GuildRole.OFFICER, MANAGE_MEMBERS_DENIED, membership -> {
            List<GuildJoinRequestEntry> pending = Tables.guildJoinRequestsTable.getRequestsForGuild(membership.getGuildId());
            return () -> {
                if (pending.isEmpty()) {
                    message(sender, "There are no pending join requests");
                    return;
                }
                message(sender, "Pending join requests (" + pending.size() + "):");
                for (GuildJoinRequestEntry request : pending) {
                    sender.sendMessage(MINI_MESSAGE.deserialize(
                            " <yellow>•</yellow> <white>" + request.getRequesterUsername()
                                    + "</white> <gray>(/guild accept " + request.getRequesterUsername()
                                    + " | /guild deny " + request.getRequesterUsername() + ")</gray>"));
                }
            };
        });
    }

    /** Sets the guild's home to the sender's current location. Admin or owner only. */
    public void setHome(Player sender) {
        // capture the location on the calling (main) thread before the async DB work
        LocationJsonEntry home = LocationJsonEntry.fromLocation(sender.getLocation());
        roleAction(sender, GuildRole.ADMIN, MANAGE_GUILD_DENIED, membership -> {
            Tables.guildsTable.setHome(membership.getGuildId(), home);
            invalidateGuild(membership.getGuildId());
            return () -> message(sender, "Set your guild's home to your current location");
        });
    }

    /** Teleports the sender to their guild's home. Available to every member. */
    public void home(Player sender) {
        runAsync(sender, () -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(sender.getUniqueId());
            if (membership == null) {
                return () -> error(sender, "You're not in a guild");
            }
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            if (guild == null || guild.getHome() == null) {
                return () -> error(sender, "Your guild has no home set — an admin or the owner can set one with /guild sethome");
            }
            LocationJsonEntry home = guild.getHome();
            return () -> {
                Location location = home.toLocation();
                if (location.getWorld() == null) {
                    error(sender, "Your guild's home is in a world that no longer exists");
                } else {
                    sender.teleport(location);
                    message(sender, "Teleported to your guild's home");
                }
            };
        });
    }

    /** Broadcasts {@code message} to every online member of the sender's guild (including the sender). */
    public void chat(Player sender, String message) {
        if (message.isBlank()) {
            error(sender, "Your message can't be empty");
            return;
        }
        // resolve the permission on the calling (main) thread; only privileged players' MiniMessage is parsed
        boolean canFormat = sender.hasPermission(CHAT_FORMAT_PERMISSION);
        runAsync(sender, () -> {
            // served from the caches, so a busy guild's chat doesn't touch the database per message
            Optional<Integer> guildId = PlayerGuildCache.INSTANCE.getSync(sender.getUniqueId());
            if (guildId.isEmpty()) {
                return () -> error(sender, "You're not in a guild");
            }
            GuildCache.CachedGuild cached = GuildCache.INSTANCE.getSync(guildId.get());
            if (cached.guild() == null) {
                return () -> error(sender, "You're not in a guild");
            }
            String guildName = cached.guild().getName();
            List<GuildMemberEntry> members = cached.members();
            return () -> {
                // privileged players may use visual MiniMessage tags (no links); everyone else's text is sent literally
                Component body = canFormat
                        ? CHAT_MINI_MESSAGE.deserialize("<white>" + message)
                        : Component.text(message, NamedTextColor.WHITE);
                Component line = MINI_MESSAGE.deserialize(
                                "<dark_green>[<green>" + guildName + "<dark_green>] <yellow>" + sender.getName() + "<gray>: ")
                        .append(body);
                for (GuildMemberEntry member : members) {
                    Player online = Bukkit.getPlayer(member.getMemberUUID());
                    if (online != null) {
                        online.sendMessage(line);
                    }
                }
            };
        });
    }

    public void info(Player sender) {
        runAsync(sender, () -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(sender.getUniqueId());
            if (membership == null) {
                return () -> error(sender, "You're not in a guild — join one with /guild join <name>");
            }
            GuildEntry guild = Tables.guildsTable.getGuildById(membership.getGuildId());
            List<GuildMemberEntry> members = Tables.guildMembersTable.getMembers(membership.getGuildId());
            return () -> {
                message(sender, "<yellow>" + (guild != null ? guild.getName() : "Your guild") + "</yellow>");
                if (guild != null && guild.getDescription() != null) {
                    sender.sendMessage(MINI_MESSAGE.deserialize(" <gray>" + guild.getDescription() + "</gray>"));
                }
                sender.sendMessage(MINI_MESSAGE.deserialize(" <gray>Members (" + members.size() + "):</gray>"));
                for (GuildMemberEntry member : members) {
                    sender.sendMessage(MINI_MESSAGE.deserialize(
                            " <gray>•</gray> <white>" + member.getMemberUsername()
                                    + "</white> <dark_gray>—</dark_gray> <gray>" + member.getRole().getReadableName() + "</gray>"));
                }
            };
        });
    }

    /**
     * Runs an action that requires the sender to be at least {@code minimum} rank in their guild. Resolves and validates
     * the membership off-thread, short-circuiting with {@code deniedMessage} if the sender is in no guild or outranked.
     */
    private void roleAction(Player sender, GuildRole minimum, String deniedMessage, Function<GuildMemberEntry, Runnable> action) {
        roleAction(sender, minimum, deniedMessage, action, null);
    }

    /** As {@link #roleAction(Player, GuildRole, String, Function)}, but runs {@code onComplete} once the action finishes. */
    private void roleAction(Player sender, GuildRole minimum, String deniedMessage, Function<GuildMemberEntry, Runnable> action, @Nullable Runnable onComplete) {
        runAsync(sender, () -> {
            GuildMemberEntry membership = Tables.guildMembersTable.getMembership(sender.getUniqueId());
            if (membership == null) {
                return () -> error(sender, "You're not in a guild");
            }
            if (!membership.getRole().isAtLeast(minimum)) {
                return () -> error(sender, deniedMessage);
            }
            return action.apply(membership);
        }, onComplete);
    }

    /** As {@link #roleAction}, but requires the {@code OWNER} role. {@code verb} completes "Only the guild owner can …". */
    private void ownerAction(Player sender, String verb, Function<GuildMemberEntry, Runnable> action) {
        roleAction(sender, GuildRole.OWNER, "Only the guild owner can " + verb, action);
    }

    /** Evicts a guild's cached data after its entry, members or roles change (a single, member-count-independent call). */
    private void invalidateGuild(int guildId) {
        GuildCache.INSTANCE.remove(guildId);
    }

    /** Members who can review and approve join requests — officer rank and above. */
    private List<GuildMemberEntry> managers(int guildId) {
        return Tables.guildMembersTable.getMembers(guildId).stream()
                .filter(member -> member.getRole().isAtLeast(GuildRole.OFFICER))
                .toList();
    }

    @Nullable
    private GuildMemberEntry findMember(int guildId, String username) {
        return Tables.guildMembersTable.getMembers(guildId).stream()
                .filter(member -> member.getMemberUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private GuildJoinRequestEntry findRequest(int guildId, String username) {
        return Tables.guildJoinRequestsTable.getRequestsForGuild(guildId).stream()
                .filter(request -> request.getRequesterUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Runs {@code work} (blocking DB + logic) off-thread, then executes the {@link Runnable} it returns on the main
     * thread for messaging. Any failure is logged and reported to {@code sender}.
     */
    private void runAsync(Player sender, Supplier<Runnable> work) {
        runAsync(sender, work, null);
    }

    /** As {@link #runAsync(Player, Supplier)}, but runs {@code onComplete} on the main thread once the action finishes. */
    private void runAsync(Player sender, Supplier<Runnable> work, @Nullable Runnable onComplete) {
        CompletableFuture.supplyAsync(work)
                .thenAccept(result -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                    result.run();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Guild command failed", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                        error(sender, "An internal error occurred");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                    return null;
                });
    }

    private void notifyIfOnline(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            message(player, message);
        }
    }

    private void message(Player player, String message) {
        new SenderMessageHandler(player).sendMessage(MINI_MESSAGE.deserialize(message));
    }

    private void error(Player player, String message) {
        new SenderMessageHandler(player).sendError(Component.text(message));
    }
}
