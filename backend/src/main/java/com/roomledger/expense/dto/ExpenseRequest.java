package com.roomledger.expense.dto;

import com.roomledger.expense.ExpenseSource;
import com.roomledger.expense.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Used for both create and update. Which split field must be filled depends on splitType:
 * EQUAL -> participants, EXACT -> shares, ITEMS -> items. The service checks that combination.
 */
public record ExpenseRequest(
        @NotBlank @Size(max = 200) String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal totalAmount,
        @NotNull UUID paidBy,
        @NotNull LocalDate expenseDate,
        @NotNull SplitType splitType,
        List<UUID> participants,
        @Valid List<ShareRequest> shares,
        @Valid @Size(max = 200) List<ItemRequest> items,
        // RECEIPT when created from a scanned receipt; MANUAL when omitted. Ignored on update.
        ExpenseSource source,
        // The version the client loaded; required on update to detect concurrent edits.
        Long version) {
}
