package com.roomledger.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What the model said, before validation. Any field can be null or nonsense (a negative price,
 * an empty name): an LLM's output is untrusted input, exactly like a request body from a user.
 */
public record ParsedReceipt(String storeName, LocalDate date, List<Item> items, BigDecimal total) {

    public record Item(String name, BigDecimal amount) {
    }
}
