package com.roomledger.group;

import com.roomledger.common.error.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single place for the rule "only members can access a group". Later features
 * (expenses, balances, settlements) call it before touching any group data.
 */
@Component
public class GroupAccess {

    private final GroupMemberRepository memberRepository;

    public GroupAccess(GroupMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Returns the caller's membership, or throws 404. Not 403: a 403 would confirm that the
     * group exists, letting outsiders probe for valid group ids. A deleted group is treated
     * exactly like one that never existed, so expenses, balances, payments and receipts of a
     * deleted group all become unreachable through this single check.
     */
    public GroupMember requireMember(UUID groupId, UUID userId) {
        return memberRepository.findActiveMembership(groupId, userId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
    }
}
