package com.roomledger.balance.dto;

import java.util.List;

/**
 * @param directDebts how many payments settling every direct debt would take (before simplification)
 * @param transfers   the simplified transfers that settle the group
 */
public record SimplifiedDebtsResponse(int directDebts, List<TransferResponse> transfers) {
}
