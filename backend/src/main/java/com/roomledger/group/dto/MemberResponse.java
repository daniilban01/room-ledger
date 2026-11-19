package com.roomledger.group.dto;

import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupRole;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(UUID userId, String displayName, GroupRole role, Instant joinedAt) {

    public static MemberResponse from(GroupMember member) {
        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getDisplayName(),
                member.getRole(),
                member.getJoinedAt());
    }
}
