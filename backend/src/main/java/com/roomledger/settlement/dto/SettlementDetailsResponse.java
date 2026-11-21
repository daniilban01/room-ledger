package com.roomledger.settlement.dto;

import com.roomledger.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A payment plus context for it.
 *
 * <p>A simplified transfer does not come from one specific expense: it settles the payer's net
 * balance, which may combine many expenses and even other people (A owes B, B owes C -> A pays C).
 * So instead of inventing a link, we show the expenses in which the payer owes money to someone else.
 */
public record SettlementDetailsResponse(SettlementResponse settlement, List<OwedExpense> payerOwesFrom) {

    /** One expense paid by someone else, and the payer's share of it. */
    public record OwedExpense(
            UUID expenseId,
            String description,
            LocalDate expenseDate,
            UserSummaryResponse paidBy,
            BigDecimal share) {
    }
}
