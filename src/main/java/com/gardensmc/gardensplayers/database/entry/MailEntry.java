package com.gardensmc.gardensplayers.database.entry;

import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single piece of mail: free-text {@code content} sent from {@code sender} to {@code recipient}. Both UUIDs reference
 * the {@code player} table.
 */
@Getter
public class MailEntry extends MySQLEntry {

    private int id;
    private UUID senderUUID;
    private UUID recipientUUID;
    private String content;
    private LocalDateTime sentAt;
    private boolean read;
    private String senderUsername;
    private String recipientUsername;

    @Override
    public MailEntry populate(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        this.senderUUID = UUID.fromString(resultSet.getString("senderUUID"));
        this.recipientUUID = UUID.fromString(resultSet.getString("recipientUUID"));
        this.content = resultSet.getString("content");
        this.sentAt = resultSet.getObject("sentAt", LocalDateTime.class);
        this.read = resultSet.getBoolean("isRead");
        this.senderUsername = resultSet.getString("senderUsername");
        this.recipientUsername = resultSet.getString("recipientUsername");
        return this;
    }
}
