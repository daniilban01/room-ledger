package com.roomledger.balance.dto;

import java.math.BigDecimal;
import java.util.List;

public record GroupBalancesResponse(BigDecimal myBalance, List<MemberBalanceResponse> members) {
}
