package com.roomledger.balance.dto;

import java.util.List;

// A wrapper object instead of a bare list, so fields can be added later without breaking clients.
public record SimplifiedDebtsResponse(List<TransferResponse> transfers) {
}
