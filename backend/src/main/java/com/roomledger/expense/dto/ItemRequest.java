package com.roomledger.expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ItemRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotEmpty Set<UUID> assignedTo) {
}
