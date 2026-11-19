package com.roomledger.group.dto;

import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupRole;

import java.util.UUID;

public record GroupSummaryResponse(UUID id, String name, GroupRole myRole) {

    public static GroupSummaryResponse from(GroupMember membership) {
        return new GroupSummaryResponse(
                membership.getGroup().getId(),
                membership.getGroup().getName(),
                membership.getRole());
    }
}
