package com.gardensmc.gardensplayers.friends;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.cache.FriendsCache;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import com.gardensmc.gardensplayers.menu.TileMenu;
import com.gardensmc.gardensplayers.menu.tile.CategoryPages;
import com.gardensmc.gardensplayers.menu.tile.FriendTileItem;
import com.gardensmc.gardensplayers.menu.tile.IncomingFriendRequestTileItem;
import com.gardensmc.gardensplayers.menu.tile.OutgoingFriendRequestTileItem;
import com.gardensmc.gardensplayers.menu.tile.TileCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Business logic for the friends system. Every action runs the blocking database work off the main thread, then
 * returns a {@link Runnable} that performs all messaging (and online-player lookups) back on the main thread.
 */
public class FriendsHandler {

    public static final FriendsHandler INSTANCE = new FriendsHandler();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private FriendsHandler() {
    }

    /**
     * Adds a friend. If the named player already has a pending request out to the sender, this accepts it; otherwise it
     * sends a new request.
     */
    public void add(Player sender, String targetName) {
        add(sender, targetName, null);
    }

    public void add(Player sender, String targetName, @Nullable Runnable onComplete) {
        runAsync(sender, () -> {
            UUID self = sender.getUniqueId();

            // If they've already requested us, adding them back accepts their request.
            FriendEntry incoming = findIncomingRequest(self, targetName);
            if (incoming != null) {
                UUID requester = incoming.getRequesterUUID();
                String requesterName = incoming.getRequesterUsername();
                Tables.friendsTable.acceptRequest(requester, self);
                invalidate(self, requester);
                return () -> {
                    message(sender, "You are now friends with " + requesterName);
                    notifyIfOnline(requester, sender.getName() + " accepted your friend request");
                };
            }

            // Otherwise send a new request.
            UUID target = Tables.playerTable.getUUID(targetName);
            if (target == null) {
                return () -> error(sender, "Player '" + targetName + "' has never joined the server");
            }
            if (target.equals(self)) {
                return () -> error(sender, "You cannot add yourself as a friend");
            }
            FriendEntry existing = Tables.friendsTable.getFriendship(self, target);
            if (existing != null) {
                if (existing.isAccepted()) {
                    return () -> error(sender, targetName + " is already your friend");
                }
                // Pending, and it isn't an incoming request (handled above), so we already sent it.
                return () -> error(sender, "You already have a pending request to " + targetName);
            }
            Tables.friendsTable.sendRequest(self, target);
            invalidate(self, target);
            return () -> {
                message(sender, "Friend request sent to " + targetName);
                notifyIfOnline(target, sender.getName() + " sent you a friend request — /friend add " + sender.getName());
            };
        }, onComplete);
    }

    public void deny(Player sender, String targetName) {
        deny(sender, targetName, null);
    }

    public void deny(Player sender, String targetName, @Nullable Runnable onComplete) {
        runAsync(sender, () -> {
            UUID self = sender.getUniqueId();
            FriendEntry request = findIncomingRequest(self, targetName);
            if (request == null) {
                return () -> error(sender, "You have no pending friend request from " + targetName);
            }
            UUID requester = request.getRequesterUUID();
            String requesterName = request.getRequesterUsername();
            Tables.friendsTable.removeFriendship(self, requester);
            invalidate(self, requester);
            return () -> message(sender, "Denied friend request from " + requesterName);
        }, onComplete);
    }

    public void cancel(Player sender, String targetName) {
        cancel(sender, targetName, null);
    }

    /** Cancels a pending request the sender previously sent to the named player. */
    public void cancel(Player sender, String targetName, @Nullable Runnable onComplete) {
        runAsync(sender, () -> {
            UUID self = sender.getUniqueId();
            FriendEntry request = findOutgoingRequest(self, targetName);
            if (request == null) {
                return () -> error(sender, "You have no pending friend request to " + targetName);
            }
            UUID addressee = request.getAddresseeUUID();
            String addresseeName = request.getAddresseeUsername();
            Tables.friendsTable.removeFriendship(self, addressee);
            invalidate(self, addressee);
            return () -> message(sender, "Cancelled friend request to " + addresseeName);
        }, onComplete);
    }

    /**
     * Finds a pending request addressed to {@code self} from the named player, matched against the same incoming-request
     * rows shown by {@code /friend requests}. Resolving the requester from the row (rather than a separate username
     * lookup) keeps accept/deny consistent with what the player was shown.
     */
    @Nullable
    private FriendEntry findIncomingRequest(UUID self, String requesterName) {
        return Tables.friendsTable.getIncomingRequests(self).stream()
                .filter(request -> request.getRequesterUsername().equalsIgnoreCase(requesterName))
                .findFirst()
                .orElse(null);
    }

    /** Counterpart to {@link #findIncomingRequest} for requests the sender has sent (matched on the addressee). */
    @Nullable
    private FriendEntry findOutgoingRequest(UUID self, String addresseeName) {
        return Tables.friendsTable.getOutgoingRequests(self).stream()
                .filter(request -> request.getAddresseeUsername().equalsIgnoreCase(addresseeName))
                .findFirst()
                .orElse(null);
    }

    public void remove(Player sender, String targetName) {
        runAsync(sender, () -> {
            UUID self = sender.getUniqueId();
            UUID target = Tables.playerTable.getUUID(targetName);
            if (target == null) {
                return () -> error(sender, "Player '" + targetName + "' has never joined the server");
            }
            FriendEntry existing = Tables.friendsTable.getFriendship(self, target);
            if (existing == null || !existing.isAccepted()) {
                return () -> error(sender, targetName + " is not your friend");
            }
            Tables.friendsTable.removeFriendship(self, target);
            invalidate(self, target);
            return () -> {
                message(sender, "Removed " + targetName + " from your friends");
                notifyIfOnline(target, sender.getName() + " removed you as a friend");
            };
        });
    }

    /** Opens the friends menu, loading friends and pending requests off-thread before building the GUI. */
    public void openMenu(Player sender) {
        UUID self = sender.getUniqueId();
        CompletableFuture
                .supplyAsync(() -> loadMenuData(self))
                .thenAccept(data -> Bukkit.getScheduler().runTask(
                        GardensPlayers.plugin,
                        () -> TileMenu.builder(sender, buildCategoryPages(data), '\u015C').open()
                ))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to open friends menu", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> error(sender, "An internal error occurred"));
                    return null;
                });
    }

    /**
     * Reloads the menu data off-thread and re-renders {@code menu} in place, preserving the open tab and page. Used as
     * the completion callback after an action mutates the friends state.
     */
    public void refreshMenu(Player viewer, TileMenu menu) {
        UUID self = viewer.getUniqueId();
        CompletableFuture
                .supplyAsync(() -> loadMenuData(self))
                .thenAccept(data -> Bukkit.getScheduler().runTask(
                        GardensPlayers.plugin,
                        () -> menu.reopenWith(buildCategoryPages(data))
                ))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to refresh friends menu", throwable);
                    return null;
                });
    }

    private FriendMenuData loadMenuData(UUID self) {
        return new FriendMenuData(
                FriendsCache.INSTANCE.getSync(self),
                Tables.friendsTable.getIncomingRequests(self),
                Tables.friendsTable.getOutgoingRequests(self)
        );
    }

    private CategoryPages buildCategoryPages(FriendMenuData data) {
        var friendsTile = new TileCategory("friends", "Your Friends", "icon_profile",
                data.friends().stream().map(FriendTileItem::new).toList());
        var incomingTile = new TileCategory("incoming", "Incoming Requests", "icon_page_left",
                data.incoming().stream().map(IncomingFriendRequestTileItem::new).toList());
        var outgoingTile = new TileCategory("outgoing", "Outgoing Requests", "icon_page_right",
                data.outgoing().stream().map(OutgoingFriendRequestTileItem::new).toList());
        return new CategoryPages(List.of(friendsTile, incomingTile, outgoingTile));
    }

    private record FriendMenuData(List<FriendEntry> friends, List<FriendEntry> incoming, List<FriendEntry> outgoing) {
    }

    public void requests(Player sender) {
        runAsync(sender, () -> {
            UUID self = sender.getUniqueId();
            List<FriendEntry> incoming = Tables.friendsTable.getIncomingRequests(self);
            List<FriendEntry> outgoing = Tables.friendsTable.getOutgoingRequests(self);
            return () -> {
                if (incoming.isEmpty() && outgoing.isEmpty()) {
                    message(sender, "You have no pending friend requests");
                    return;
                }
                if (!incoming.isEmpty()) {
                    message(sender, "Incoming requests (" + incoming.size() + "):");
                    for (FriendEntry request : incoming) {
                        sender.sendMessage(MINI_MESSAGE.deserialize(
                                " <yellow>←</yellow> <white>" + request.getRequesterUsername()
                                        + "</white> <gray>(/friend accept " + request.getRequesterUsername() + ")</gray>"));
                    }
                }
                if (!outgoing.isEmpty()) {
                    message(sender, "Outgoing requests (" + outgoing.size() + "):");
                    for (FriendEntry request : outgoing) {
                        sender.sendMessage(MINI_MESSAGE.deserialize(
                                " <gray>→ " + request.getAddresseeUsername() + " (pending)</gray>"));
                    }
                }
            };
        });
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
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Friend command failed", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> {
                        error(sender, "An internal error occurred");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                    return null;
                });
    }

    /** Drops both players' cached friend lists, since an accepted friendship appears in both. */
    private void invalidate(UUID a, UUID b) {
        FriendsCache.INSTANCE.remove(a);
        FriendsCache.INSTANCE.remove(b);
    }

    private void notifyIfOnline(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            message(player, message);
        }
    }

    private void message(Player player, String message) {
        new SenderMessageHandler(player).sendMessage(Component.text(message));
    }

    private void error(Player player, String message) {
        new SenderMessageHandler(player).sendError(Component.text(message));
    }
}
