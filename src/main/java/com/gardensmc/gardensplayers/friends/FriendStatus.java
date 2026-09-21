package com.gardensmc.gardensplayers.friends;

/**
 * State of a row in the {@code friends} table. Stored as its {@link #name()} in the {@code status} column.
 */
public enum FriendStatus {
    PENDING,
    ACCEPTED
}
