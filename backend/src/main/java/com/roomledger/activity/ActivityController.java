package com.roomledger.activity;

import com.roomledger.activity.dto.ActivityResponse;
import com.roomledger.auth.AuthenticatedUser;
import com.roomledger.common.PageResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public PageResponse<ActivityResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @PathVariable UUID groupId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "30") int size) {
        return activityService.list(groupId, currentUser.id(), page, size);
    }
}
