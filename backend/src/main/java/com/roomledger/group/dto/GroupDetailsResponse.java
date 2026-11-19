package com.roomledger.group.dto;

import com.roomledger.group.Group;
import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupRole;

import java.util.List;
import java.util.UUID;

public record GroupDetailsResponse(
        UUID id,
        String name,
        String joinCode,
        GroupRole myRole,
        List<MemberResponse> members) {

    public static GroupDetailsResponse from(Group group, GroupRole myRole, List<GroupMember> members) {
        return new GroupDetailsResponse(
                group.getId(),
                group.getName(),
                group.getJoinCode(),
                myRole,
                members.stream().map(MemberResponse::from).toList());
    }
}
