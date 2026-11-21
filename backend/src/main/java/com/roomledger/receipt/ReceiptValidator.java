package com.roomledger.receipt;

import com.roomledger.receipt.dto.ReceiptDraftResponse;
import com.roomledger.receipt.dto.ReceiptDraftResponse.Code;
import com.roomledger.receipt.dto.ReceiptDraftResponse.Warning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Business validation of what the model read. Pure Java, no Spring, easy to unit test.
 *
 * <p>Philosophy: be forgiving where the user will review anyway (flag, don't fail), but never pass
 * on values that cannot be right. A wrong total only produces a warning; a negative price is removed.
 */
public class ReceiptValidator {

    // A shared flat's receipt line above this is far more likely a misread than a real purchase.
    static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");
    static final BigDecimal MISMATCH_TOLERANCE = new BigDecimal("0.05");
    private static final int MAX_NAME_LENGTH = 200;

    public ReceiptDraftResponse validate(ParsedReceipt receipt) {
        List<Warning> warnings = new ArrayList<>();
        List<ReceiptDraftResponse.Item> items = new ArrayList<>();
        int dropped = 0;

        for (ParsedReceipt.Item item : receipt.items() == null ? List.<ParsedReceipt.Item>of() : receipt.items()) {
            BigDecimal amount = validAmount(item.amount());
            if (item.name() == null || amount == null) {
                dropped++;
                continue;
            }
            items.add(new ReceiptDraftResponse.Item(truncate(item.name()), amount));
        }

        if (items.isEmpty()) {
            throw new UnreadableReceiptException("No items could be read from the receipt");
        }
        if (dropped > 0) {
            warnings.add(new Warning(Code.ITEMS_DROPPED, dropped + " line(s) had no name or an invalid price and were skipped"));
        }

        BigDecimal itemsTotal = items.stream().map(ReceiptDraftResponse.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = validAmount(receipt.total());
        if (total == null) {
            warnings.add(new Warning(Code.TOTAL_MISSING, "The receipt total could not be read"));
        } else if (itemsTotal.subtract(total).abs().compareTo(MISMATCH_TOLERANCE) > 0) {
            warnings.add(new Warning(Code.TOTAL_MISMATCH,
                    "Items add up to %s but the receipt total is %s".formatted(itemsTotal, total)));
        }

        String storeName = receipt.storeName() == null ? null : truncate(receipt.storeName());
        return new ReceiptDraftResponse(storeName, receipt.date(), items, total, itemsTotal, warnings);
    }

    /** Positive, not absurd, rounded to cents (models sometimes return 3.999 for 4.00). Null otherwise. */
    private static BigDecimal validAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0 || rounded.compareTo(MAX_AMOUNT) > 0) {
            return null;
        }
        return rounded;
    }

    private static String truncate(String text) {
        String trimmed = text.trim();
        return trimmed.length() <= MAX_NAME_LENGTH ? trimmed : trimmed.substring(0, MAX_NAME_LENGTH);
    }
}
