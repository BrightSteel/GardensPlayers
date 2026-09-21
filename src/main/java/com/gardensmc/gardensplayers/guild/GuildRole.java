package com.gardensmc.gardensplayers.guild;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * A member's rank within a guild. Stored as its {@link #name()} in the {@code guildMembers.role} column.
 *
 * <p>Declared most-senior first, so a lower {@link #ordinal()} means a more senior rank. Seniority is cumulative: each
 * rank can do everything the ranks below it can, plus more — {@code OFFICER} manages members, {@code ADMIN} adds home
 * and description control, and {@code OWNER} adds disband and rank management on top.
 */
@Getter
public enum GuildRole {
    OWNER("Owner"),
    ADMIN("Admin"),
    OFFICER("Officer"),
    MEMBER("Member");

    /** Human-friendly capitalised name for display in chat and menus. */
    private final String readableName;

    GuildRole(String readableName) {
        this.readableName = readableName;
    }

    /** True if this role is at least as senior as {@code other}. */
    public boolean isAtLeast(GuildRole other) {
        return this.ordinal() <= other.ordinal();
    }

    /** True if this role is strictly more senior than {@code other} (e.g. who a member is allowed to kick). */
    public boolean isHigherThan(GuildRole other) {
        return this.ordinal() < other.ordinal();
    }

    /** The next rank up, or {@code null} if there's no higher promotable rank (ownership is handed over via transfer). */
    @Nullable
    public GuildRole promoted() {
        return switch (this) {
            case MEMBER -> OFFICER;
            case OFFICER -> ADMIN;
            default -> null;
        };
    }

    /** The next rank down, or {@code null} if there's no lower rank. */
    @Nullable
    public GuildRole demoted() {
        return switch (this) {
            case ADMIN -> OFFICER;
            case OFFICER -> MEMBER;
            default -> null;
        };
    }
}
