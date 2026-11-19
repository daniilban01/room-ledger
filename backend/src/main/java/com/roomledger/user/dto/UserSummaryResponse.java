package com.roomledger.user.dto;

import com.roomledger.user.User;

import java.util.UUID;

/** Minimal public view of a user, safe to show to other group members (no email). */
public record UserSummaryResponse(UUID id, String displayName) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getDisplayName());
    }
}
