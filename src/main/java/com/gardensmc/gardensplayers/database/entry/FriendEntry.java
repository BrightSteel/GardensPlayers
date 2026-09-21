package com.gardensmc.gardensplayers.database.entry;

import com.gardensmc.gardensplayers.friends.FriendStatus;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single directed friendship row. {@code requester} sent the request to {@code addressee}; {@code status} is
 * {@link FriendStatus#PENDING} until accepted. Usernames are populated via a join on the {@code player} table so
 * relationships can be rendered without extra lookups.
 */
@Getter
public class FriendEntry extends MySQLEntry {

    private UUID requesterUUID;
    private UUID addresseeUUID;
    private String status;
    private LocalDateTime createdAt;
    @Nullable
    private LocalDateTime acceptedAt;
    private String requesterUsername;
    private String addresseeUsername;

    @Override
    public FriendEntry populate(ResultSet resultSet) throws SQLException {
        this.requesterUUID = UUID.fromString(resultSet.getString("requesterUUID"));
        this.addresseeUUID = UUID.fromString(resultSet.getString("addresseeUUID"));
        this.status = resultSet.getString("status");
        this.createdAt = resultSet.getObject("createdAt", LocalDateTime.class);
        this.acceptedAt = resultSet.getObject("acceptedAt", LocalDateTime.class);
        this.requesterUsername = resultSet.getString("requesterUsername");
        this.addresseeUsername = resultSet.getString("addresseeUsername");
        return this;
    }

    public boolean isPending() {
        return FriendStatus.PENDING.name().equals(status);
    }

    public boolean isAccepted() {
        return FriendStatus.ACCEPTED.name().equals(status);
    }

    /** The UUID of the party that isn't {@code self}. */
    public UUID otherUUID(UUID self) {
        return self.equals(requesterUUID) ? addresseeUUID : requesterUUID;
    }

    /** The username of the party that isn't {@code self}. */
    public String otherUsername(UUID self) {
        return self.equals(requesterUUID) ? addresseeUsername : requesterUsername;
    }
}
