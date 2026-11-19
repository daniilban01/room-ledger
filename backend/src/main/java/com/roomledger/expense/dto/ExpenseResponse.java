package com.roomledger.expense.dto;

import com.roomledger.expense.Expense;
import com.roomledger.expense.ExpenseItem;
import com.roomledger.expense.ExpenseShare;
import com.roomledger.expense.ExpenseSource;
import com.roomledger.expense.SplitCalculator;
import com.roomledger.expense.SplitType;
import com.roomledger.user.User;
import com.roomledger.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        String description,
        BigDecimal totalAmount,
        UserSummaryResponse paidBy,
        LocalDate expenseDate,
        SplitType splitType,
        ExpenseSource source,
        UserSummaryResponse createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<Share> shares,
        List<Item> items) {

    public record Share(UUID userId, String displayName, BigDecimal amount) {
    }

    public record Item(UUID id, String name, BigDecimal amount, List<UUID> assignedTo) {
    }

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getTotalAmount(),
                UserSummaryResponse.from(expense.getPaidBy()),
                expense.getExpenseDate(),
                expense.getSplitType(),
                expense.getSource(),
                UserSummaryResponse.from(expense.getCreatedBy()),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.getVersion(),
                expense.getShares().stream()
                        .sorted(Comparator.comparing((ExpenseShare share) -> share.getUser().getId(), SplitCalculator.STABLE_ORDER))
                        .map(share -> new Share(share.getUser().getId(), share.getUser().getDisplayName(), share.getAmount()))
                        .toList(),
                expense.getItems().stream().map(ExpenseResponse::toItem).toList());
    }

    private static Item toItem(ExpenseItem item) {
        List<UUID> assignees = item.getAssignedTo().stream()
                .map(User::getId)
                .sorted(SplitCalculator.STABLE_ORDER)
                .toList();
        return new Item(item.getId(), item.getName(), item.getAmount(), assignees);
    }
}
