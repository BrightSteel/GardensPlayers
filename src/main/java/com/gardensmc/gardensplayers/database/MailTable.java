package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.MailEntry;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MailTable implements Table {

    /** Shared projection: every {@link MailEntry} read carries both players' usernames via a join on the player table. */
    private static final String BASE_SELECT = """
            SELECT mail.*, sender.username AS senderUsername, recipient.username AS recipientUsername
            FROM mail
            JOIN player sender ON mail.senderUUID = sender.uuid
            JOIN player recipient ON mail.recipientUUID = recipient.uuid
            """;

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS mail(
                    id int NOT NULL AUTO_INCREMENT,
                    senderUUID varchar(36) NOT NULL,
                    recipientUUID varchar(36) NOT NULL,
                    content text NOT NULL,
                    sentAt datetime NOT NULL,
                    isRead boolean NOT NULL DEFAULT false,
                    PRIMARY KEY (id),
                    FOREIGN KEY (senderUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (recipientUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    INDEX recipientIdx (recipientUUID)
                )
               """;
    }

    public void sendMail(UUID sender, UUID recipient, String content) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO mail (senderUUID, recipientUUID, content, sentAt) VALUES (?, ?, ?, ?)",
                    sender.toString(),
                    recipient.toString(),
                    content,
                    LocalDateTime.now(Clock.systemUTC())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MailEntry> getReceivedMail(UUID recipient) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE mail.recipientUUID = ? ORDER BY mail.sentAt DESC",
                    MailEntry::new,
                    recipient.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MailEntry> getSentMail(UUID sender) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE mail.senderUUID = ? ORDER BY mail.sentAt DESC",
                    MailEntry::new,
                    sender.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MailEntry> getUnreadMail(UUID recipient) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE mail.recipientUUID = ? AND mail.isRead = false ORDER BY mail.sentAt DESC",
                    MailEntry::new,
                    recipient.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void markAsRead(int id) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE mail SET isRead = true WHERE id = ? LIMIT 1",
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMail(int id) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM mail WHERE id = ? LIMIT 1",
                    id
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearMail(UUID recipient) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM mail WHERE recipientUUID = ?",
                    recipient.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
