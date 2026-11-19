package com.roomledger.balance;

import java.math.BigDecimal;
import java.util.UUID;

/** One row of an aggregate query: a user and a summed amount. */
public record UserAmount(UUID userId, BigDecimal amount) {
}
