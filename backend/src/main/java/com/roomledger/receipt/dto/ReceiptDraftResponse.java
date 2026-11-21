package com.roomledger.receipt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A validated receipt, NOT saved anywhere. The user reviews it, assigns items to people
 * and only then creates a normal ITEMS expense from it.
 *
 * @param total      the total printed on the receipt, if it could be read
 * @param itemsTotal the sum of the items below; differs from {@code total} when a line was misread
 */
public record ReceiptDraftResponse(
        String storeName,
        LocalDate date,
        List<Item> items,
        BigDecimal total,
        BigDecimal itemsTotal,
        List<Warning> warnings) {

    public record Item(String name, BigDecimal amount) {
    }

    /** A machine-readable code (the frontend shows its own text for it) plus an English explanation. */
    public record Warning(Code code, String message) {
    }

    public enum Code {
        ITEMS_DROPPED,
        TOTAL_MISSING,
        TOTAL_MISMATCH
    }
}
