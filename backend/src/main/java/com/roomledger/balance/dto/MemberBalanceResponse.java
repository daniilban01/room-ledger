package com.roomledger.balance.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** balance > 0: the member is owed money; balance < 0: the member owes money. */
public record MemberBalanceResponse(UUID userId, String displayName, BigDecimal balance) {
}
