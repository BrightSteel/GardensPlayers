package com.gardensmc.gardensplayers.mail;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.gardensmc.gardensplayers.command.SenderMessageHandler;
import com.gardensmc.gardensplayers.database.Tables;
import com.gardensmc.gardensplayers.database.entry.MailEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Business logic for reading mail. Database work runs off the main thread; messaging happens back on it.
 */
public class MailHandler {

    public static final MailHandler INSTANCE = new MailHandler();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final int PAGE_SIZE = 5;

    private MailHandler() {
    }

    /**
     * Shows one page of the player's received mail (unread first, then newest), and marks the messages on that page as
     * read.
     */
    public void read(Player player, int page) {
        UUID self = player.getUniqueId();
        CompletableFuture
                .supplyAsync(() -> {
                    List<MailEntry> mail = Tables.mailTable.getReceivedMail(self);
                    pageSlice(mail, page).stream()
                            .filter(entry -> !entry.isRead())
                            .forEach(entry -> Tables.mailTable.markAsRead(entry.getId()));
                    return mail;
                })
                .thenAccept(mail -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> displayPage(player, mail, page)))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to read mail", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin,
                            () -> new SenderMessageHandler(player).sendError("An internal error occurred"));
                    return null;
                });
    }

    /** Sends mail to the named player, notifying them in-game if they're online. */
    public void send(Player sender, String recipientName, String content) {
        if (content.isBlank()) {
            new SenderMessageHandler(sender).sendError("Your message cannot be empty");
            return;
        }
        UUID self = sender.getUniqueId();
        String senderName = sender.getName();
        // Resolve online players directly for their exact UUID (also handles self-mail and name/casing mismatches);
        // fall back to a username lookup for offline recipients. Bukkit.getPlayer must run on the main thread.
        Player onlineRecipient = Bukkit.getPlayer(recipientName);
        UUID onlineRecipientUUID = onlineRecipient != null ? onlineRecipient.getUniqueId() : null;
        CompletableFuture
                .<Runnable>supplyAsync(() -> {
                    UUID recipient = onlineRecipientUUID != null
                            ? onlineRecipientUUID
                            : Tables.playerTable.getUUID(recipientName);
                    if (recipient == null) {
                        return () -> new SenderMessageHandler(sender)
                                .sendError("Player '" + recipientName + "' has never joined the server");
                    }
                    Tables.mailTable.sendMail(self, recipient, content);
                    return () -> {
                        new SenderMessageHandler(sender).sendMessage("Mail sent to " + recipientName);
                        notifyIfOnline(recipient, senderName);
                    };
                })
                .thenAccept(result -> Bukkit.getScheduler().runTask(GardensPlayers.plugin, result))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to send mail", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin,
                            () -> new SenderMessageHandler(sender).sendError("An internal error occurred"));
                    return null;
                });
    }

    private void notifyIfOnline(UUID recipientUUID, String senderName) {
        Player recipient = Bukkit.getPlayer(recipientUUID);
        if (recipient != null) {
            recipient.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>You have new mail from <white>" + senderName + "<green>! Read it with <gray>/mail"));
        }
    }

    /** Deletes all of the player's received mail. */
    public void clear(Player player) {
        UUID self = player.getUniqueId();
        CompletableFuture
                .runAsync(() -> Tables.mailTable.clearMail(self))
                .thenRun(() -> Bukkit.getScheduler().runTask(GardensPlayers.plugin,
                        () -> new SenderMessageHandler(player).sendMessage("Cleared your mail")))
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to clear mail", throwable);
                    Bukkit.getScheduler().runTask(GardensPlayers.plugin,
                            () -> new SenderMessageHandler(player).sendError("An internal error occurred"));
                    return null;
                });
    }

    private void displayPage(Player player, List<MailEntry> mail, int page) {
        var messageHandler = new SenderMessageHandler(player);
        if (mail.isEmpty()) {
            messageHandler.sendMessage("You have no mail");
            return;
        }
        int totalPages = (int) Math.ceil(mail.size() / (double) PAGE_SIZE);
        if (page < 1 || page > totalPages) {
            messageHandler.sendError("Page " + page + " doesn't exist (" + totalPages + " total)");
            return;
        }
        messageHandler.sendMessage("Your mail (page " + page + "/" + totalPages + "):");
        for (MailEntry entry : pageSlice(mail, page)) {
            // Build the line from plain components so mail content can't inject formatting.
            Component marker = entry.isRead() ? Component.empty() : Component.text("● ", NamedTextColor.GREEN);
            player.sendMessage(marker
                    .append(Component.text(DATE_FORMAT.format(entry.getSentAt().atOffset(ZoneOffset.UTC)) + " ", NamedTextColor.GRAY))
                    .append(Component.text(entry.getSenderUsername(), NamedTextColor.WHITE))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(entry.getContent(), NamedTextColor.WHITE)));
        }
        if (page < totalPages) {
            messageHandler.sendMessage("Use /mail " + (page + 1) + " for the next page");
        }
    }

    private List<MailEntry> pageSlice(List<MailEntry> mail, int page) {
        int from = (page - 1) * PAGE_SIZE;
        if (from < 0 || from >= mail.size()) {
            return List.of();
        }
        return mail.subList(from, Math.min(from + PAGE_SIZE, mail.size()));
    }

    /** If the player has unread mail, lets them know how to read it. Safe to call on the main thread (runs async). */
    public void alertUnread(Player player) {
        UUID self = player.getUniqueId();
        CompletableFuture
                .supplyAsync(() -> Tables.mailTable.getUnreadMail(self).size())
                .thenAccept(count -> {
                    if (count > 0) {
                        Bukkit.getScheduler().runTask(GardensPlayers.plugin, () -> player.sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        "<green>You have <white>" + count + "<green> unread mail! Read it with <gray>/mail")));
                    }
                })
                .exceptionally(throwable -> {
                    GardensPlayers.plugin.getLogger().log(Level.SEVERE, "Failed to check unread mail", throwable);
                    return null;
                });
    }
}
