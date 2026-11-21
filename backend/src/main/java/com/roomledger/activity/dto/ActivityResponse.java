package com.roomledger.activity.dto;

import com.roomledger.activity.ActivityEvent;
import com.roomledger.activity.ActivityType;
import com.roomledger.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        ActivityType type,
        UserSummaryResponse actor,
        Map<String, Object> payload,
        Instant createdAt) {

    public static ActivityResponse from(ActivityEvent event) {
        return new ActivityResponse(
                event.getId(),
                event.getType(),
                UserSummaryResponse.from(event.getActor()),
                event.getPayload(),
                event.getCreatedAt());
    }
}
