package com.roomledger.group;

import com.roomledger.activity.ActivityService;
import com.roomledger.activity.ActivityType;
import com.roomledger.common.error.ForbiddenException;
import com.roomledger.common.error.NotFoundException;
import com.roomledger.group.dto.CreateGroupRequest;
import com.roomledger.group.dto.GroupDetailsResponse;
import com.roomledger.group.dto.GroupSummaryResponse;
import com.roomledger.user.User;
import com.roomledger.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GroupService {

    private static final int MAX_JOIN_CODE_ATTEMPTS = 5;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GroupAccess groupAccess;
    private final JoinCodeGenerator joinCodeGenerator;
    private final ActivityService activityService;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository memberRepository,
                        UserRepository userRepository,
                        GroupAccess groupAccess,
                        JoinCodeGenerator joinCodeGenerator,
                        ActivityService activityService) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.groupAccess = groupAccess;
        this.joinCodeGenerator = joinCodeGenerator;
        this.activityService = activityService;
    }

    @Transactional
    public GroupDetailsResponse create(UUID userId, CreateGroupRequest request) {
        // getReferenceById returns a proxy without a SELECT; we only need the id for the foreign key.
        User creator = userRepository.getReferenceById(userId);
        Group group = groupRepository.save(new Group(request.name().trim(), newUniqueJoinCode(), creator));
        GroupMember membership = memberRepository.save(new GroupMember(group, creator, GroupRole.ADMIN));
        activityService.record(group, creator, ActivityType.GROUP_CREATED, Map.of("groupName", group.getName()));
        return toDetails(membership);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listForUser(UUID userId) {
        return memberRepository.findAllByUserIdWithGroup(userId).stream()
                .map(GroupSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse get(UUID groupId, UUID userId) {
        return toDetails(groupAccess.requireMember(groupId, userId));
    }

    /** Joining a group you are already in returns the group instead of failing (idempotent). */
    @Transactional
    public GroupDetailsResponse join(UUID userId, String joinCode) {
        Group group = groupRepository.findByJoinCode(joinCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Invalid join code"));

        Optional<GroupMember> existing = memberRepository.findByGroupIdAndUserId(group.getId(), userId);
        if (existing.isPresent()) {
            return toDetails(existing.get());
        }

        User user = userRepository.getReferenceById(userId);
        GroupMember membership = memberRepository.save(new GroupMember(group, user, GroupRole.MEMBER));
        activityService.record(group, user, ActivityType.MEMBER_JOINED, Map.of());
        return toDetails(membership);
    }

    /** Invalidates the old code, e.g. after it was shared with the wrong person. */
    @Transactional
    public GroupDetailsResponse regenerateJoinCode(UUID groupId, UUID userId) {
        GroupMember membership = groupAccess.requireMember(groupId, userId);
        if (!membership.isAdmin()) {
            throw new ForbiddenException("Only group admins can regenerate the join code");
        }
        // No save() needed: the group is a managed entity, Hibernate writes the change on commit.
        membership.getGroup().changeJoinCode(newUniqueJoinCode());
        return toDetails(membership);
    }

    private GroupDetailsResponse toDetails(GroupMember membership) {
        Group group = membership.getGroup();
        List<GroupMember> members = memberRepository.findAllByGroupIdWithUser(group.getId());
        return GroupDetailsResponse.from(group, membership.getRole(), members);
    }

    private String newUniqueJoinCode() {
        // With 31^8 (~850 billion) possible codes a collision is very unlikely, but cheap to rule out.
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.generate();
            if (!groupRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique join code");
    }
}
