package com.gardensmc.gardensplayers.database;

import com.gardensmc.gardensplayers.database.entry.FriendEntry;
import com.gardensmc.gardensplayers.friends.FriendStatus;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FriendsTable implements Table {

    /**
     * Shared projection: every {@link FriendEntry} read carries both players' usernames via an inner join on the
     * {@code player} table (guaranteed to exist by the foreign keys).
     */
    private static final String BASE_SELECT = """
            SELECT friends.requesterUUID, friends.addresseeUUID, friends.status,
                   friends.createdAt, friends.acceptedAt,
                   req.username AS requesterUsername, addr.username AS addresseeUsername
            FROM friends
            JOIN player req ON friends.requesterUUID = req.uuid
            JOIN player addr ON friends.addresseeUUID = addr.uuid
            """;

    @Override
    public String getPrepareStatement() {
        return """
                CREATE TABLE IF NOT EXISTS friends(
                    requesterUUID varchar(36) NOT NULL,
                    addresseeUUID varchar(36) NOT NULL,
                    status varchar(20) NOT NULL DEFAULT 'PENDING',
                    createdAt datetime NOT NULL,
                    acceptedAt datetime,
                    PRIMARY KEY (requesterUUID, addresseeUUID),
                    FOREIGN KEY (requesterUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (addresseeUUID) REFERENCES player(uuid) ON DELETE CASCADE,
                    INDEX addresseeIdx (addresseeUUID),
                    CONSTRAINT noSelfFriend CHECK (requesterUUID <> addresseeUUID)
                )
               """;
    }

    /**
     * Returns the relationship between two players regardless of who requested whom, or {@code null} if none exists.
     */
    @Nullable
    public FriendEntry getFriendship(UUID a, UUID b) {
        try {
            return DatabaseConnector.INSTANCE.querySingleton(
                    BASE_SELECT + "WHERE (friends.requesterUUID = ? AND friends.addresseeUUID = ?) " +
                            "OR (friends.requesterUUID = ? AND friends.addresseeUUID = ?) LIMIT 1",
                    FriendEntry::new,
                    a.toString(), b.toString(), b.toString(), a.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FriendEntry> getFriends(UUID uuid) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE friends.status = 'ACCEPTED' " +
                            "AND (friends.requesterUUID = ? OR friends.addresseeUUID = ?) " +
                            "ORDER BY friends.acceptedAt DESC",
                    FriendEntry::new,
                    uuid.toString(), uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FriendEntry> getIncomingRequests(UUID uuid) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE friends.status = 'PENDING' AND friends.addresseeUUID = ? " +
                            "AND friends.requesterUUID <> friends.addresseeUUID " +
                            "ORDER BY friends.createdAt DESC",
                    FriendEntry::new,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FriendEntry> getOutgoingRequests(UUID uuid) {
        try {
            return DatabaseConnector.INSTANCE.queryCollection(
                    BASE_SELECT + "WHERE friends.status = 'PENDING' AND friends.requesterUUID = ? " +
                            "AND friends.requesterUUID <> friends.addresseeUUID " +
                            "ORDER BY friends.createdAt DESC",
                    FriendEntry::new,
                    uuid.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendRequest(UUID requester, UUID addressee) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "INSERT INTO friends (requesterUUID, addresseeUUID, status, createdAt) VALUES (?, ?, ?, ?)",
                    requester.toString(),
                    addressee.toString(),
                    FriendStatus.PENDING.name(),
                    LocalDateTime.now(Clock.systemUTC())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void acceptRequest(UUID requester, UUID addressee) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "UPDATE friends SET status = ?, acceptedAt = ? " +
                            "WHERE requesterUUID = ? AND addresseeUUID = ? AND status = 'PENDING'",
                    FriendStatus.ACCEPTED.name(),
                    LocalDateTime.now(Clock.systemUTC()),
                    requester.toString(),
                    addressee.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Deletes the relationship in either direction. Used for unfriend and for declining a request. */
    public void removeFriendship(UUID a, UUID b) {
        try {
            DatabaseConnector.INSTANCE.executeUpdate(
                    "DELETE FROM friends WHERE (requesterUUID = ? AND addresseeUUID = ?) " +
                            "OR (requesterUUID = ? AND addresseeUUID = ?)",
                    a.toString(), b.toString(), b.toString(), a.toString()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
