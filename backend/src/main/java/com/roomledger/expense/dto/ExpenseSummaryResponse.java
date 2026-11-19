package com.roomledger.expense.dto;

import com.roomledger.expense.Expense;
import com.roomledger.expense.SplitType;
import com.roomledger.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Row in the expense list: no shares/items, so the list needs a single query. */
public record ExpenseSummaryResponse(
        UUID id,
        String description,
        BigDecimal totalAmount,
        UserSummaryResponse paidBy,
        LocalDate expenseDate,
        SplitType splitType) {

    public static ExpenseSummaryResponse from(Expense expense) {
        return new ExpenseSummaryResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getTotalAmount(),
                UserSummaryResponse.from(expense.getPaidBy()),
                expense.getExpenseDate(),
                expense.getSplitType());
    }
}
