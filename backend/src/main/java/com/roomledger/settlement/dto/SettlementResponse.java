package com.roomledger.settlement.dto;

import com.roomledger.settlement.Settlement;
import com.roomledger.settlement.SettlementStatus;
import com.roomledger.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UserSummaryResponse from,
        UserSummaryResponse to,
        BigDecimal amount,
        String note,
        SettlementStatus status,
        Instant createdAt,
        Instant resolvedAt,
        long version) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                UserSummaryResponse.from(settlement.getFromUser()),
                UserSummaryResponse.from(settlement.getToUser()),
                settlement.getAmount(),
                settlement.getNote(),
                settlement.getStatus(),
                settlement.getCreatedAt(),
                settlement.getResolvedAt(),
                settlement.getVersion());
    }
}
