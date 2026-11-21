package com.roomledger.receipt;

import com.roomledger.receipt.dto.ReceiptDraftResponse;
import com.roomledger.receipt.dto.ReceiptDraftResponse.Code;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptValidatorTest {

    private final ReceiptValidator validator = new ReceiptValidator();

    private static ParsedReceipt.Item item(String name, String amount) {
        return new ParsedReceipt.Item(name, amount == null ? null : new BigDecimal(amount));
    }

    private static ParsedReceipt receipt(String total, ParsedReceipt.Item... items) {
        return new ParsedReceipt("Magazin", null, List.of(items), total == null ? null : new BigDecimal(total));
    }

    private static List<Code> codes(ReceiptDraftResponse draft) {
        return draft.warnings().stream().map(ReceiptDraftResponse.Warning::code).toList();
    }

    @Test
    void acceptsAConsistentReceiptWithoutWarnings() {
        ReceiptDraftResponse draft = validator.validate(receipt("12.00", item("Lapte", "7.50"), item("Pâine", "4.50")));

        assertThat(draft.items()).hasSize(2);
        assertThat(draft.itemsTotal()).isEqualByComparingTo("12.00");
        assertThat(draft.warnings()).isEmpty();
    }

    @Test
    void flagsButKeepsAReceiptWhoseItemsDoNotMatchTheTotal() {
        ReceiptDraftResponse draft = validator.validate(receipt("15.00", item("Lapte", "7.50"), item("Pâine", "4.50")));

        assertThat(codes(draft)).containsExactly(Code.TOTAL_MISMATCH);
        assertThat(draft.items()).hasSize(2);
    }

    @Test
    void toleratesUpToFiveBaniDifference() {
        ReceiptDraftResponse draft = validator.validate(receipt("12.05", item("Lapte", "7.50"), item("Pâine", "4.50")));

        assertThat(draft.warnings()).isEmpty();
    }

    @Test
    void dropsNegativeZeroAbsurdAndNamelessItems() {
        ReceiptDraftResponse draft = validator.validate(receipt("7.50",
                item("Lapte", "7.50"),
                item("Reducere", "-2.00"),
                item("Pungă", "0"),
                item("Televizor?", "999999.00"),
                item(null, "3.00"),
                item("Fără preț", null)));

        assertThat(draft.items()).extracting(ReceiptDraftResponse.Item::name).containsExactly("Lapte");
        assertThat(codes(draft)).containsExactly(Code.ITEMS_DROPPED);
    }

    @Test
    void roundsAmountsToCents() {
        ReceiptDraftResponse draft = validator.validate(receipt(null, item("Mere", "6.3249")));

        assertThat(draft.items().getFirst().amount()).isEqualByComparingTo("6.32");
        assertThat(codes(draft)).containsExactly(Code.TOTAL_MISSING);
    }

    @Test
    void failsWhenNothingUsableWasRead() {
        assertThatThrownBy(() -> validator.validate(receipt("10.00", item("X", "-1"))))
                .isInstanceOf(UnreadableReceiptException.class);
        assertThatThrownBy(() -> validator.validate(new ParsedReceipt(null, null, null, null)))
                .isInstanceOf(UnreadableReceiptException.class);
    }
}
