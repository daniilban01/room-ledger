package com.roomledger.expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareRequest(
        @NotNull UUID userId,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal amount) {
}
