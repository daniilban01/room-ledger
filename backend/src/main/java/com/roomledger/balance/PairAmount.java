package com.roomledger.balance;

import java.math.BigDecimal;
import java.util.UUID;

/** An aggregate amount between two users, e.g. "first owes second" or "first paid second". */
public record PairAmount(UUID first, UUID second, BigDecimal amount) {
}
