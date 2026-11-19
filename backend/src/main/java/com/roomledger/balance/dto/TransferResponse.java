package com.roomledger.balance.dto;

import com.roomledger.user.dto.UserSummaryResponse;

import java.math.BigDecimal;

/** A suggested payment: {@code from} should pay {@code amount} to {@code to}. */
public record TransferResponse(UserSummaryResponse from, UserSummaryResponse to, BigDecimal amount) {
}
