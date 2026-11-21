package com.roomledger.activity;

import com.roomledger.activity.dto.ActivityResponse;
import com.roomledger.common.PageResponse;
import com.roomledger.group.Group;
import com.roomledger.group.GroupAccess;
import com.roomledger.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ActivityService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ActivityRepository activityRepository;
    private final GroupAccess groupAccess;

    public ActivityService(ActivityRepository activityRepository, GroupAccess groupAccess) {
        this.activityRepository = activityRepository;
        this.groupAccess = groupAccess;
    }

    /**
     * Writes the event in the SAME transaction as the action it describes: if the action rolls back,
     * so does the event, and the log can never claim something that did not happen (or miss something
     * that did). MANDATORY makes Spring throw if this is ever called outside a transaction.
     *
     * <p>Alternative considered: publishing Spring events handled after commit. That decouples the
     * code, but an event lost after a successful commit would leave a gap in the history.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Group group, User actor, ActivityType type, Map<String, Object> payload) {
        activityRepository.save(new ActivityEvent(group, actor, type, payload));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(UUID groupId, UUID userId, int page, int size) {
        groupAccess.requireMember(groupId, userId);
        var pageRequest = PageRequest.of(
                Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by(Sort.Order.desc("createdAt")));
        return PageResponse.from(activityRepository.findByGroupId(groupId, pageRequest), ActivityResponse::from);
    }
}
