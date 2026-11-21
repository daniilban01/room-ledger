package com.roomledger.receipt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptJsonReaderTest {

    private final ReceiptJsonReader reader = new ReceiptJsonReader();

    @Test
    void readsTheExpectedShape() {
        ParsedReceipt receipt = reader.read("""
                {"storeName": "Kaufland", "date": "2026-11-20",
                 "items": [{"name": "Lapte", "amount": 7.49}, {"name": "Pâine", "amount": 4.5}],
                 "total": 11.99}
                """);

        assertThat(receipt.storeName()).isEqualTo("Kaufland");
        assertThat(receipt.date()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(receipt.items()).containsExactly(
                new ParsedReceipt.Item("Lapte", new BigDecimal("7.49")),
                new ParsedReceipt.Item("Pâine", new BigDecimal("4.5")));
        assertThat(receipt.total()).isEqualByComparingTo("11.99");
    }

    @Test
    void toleratesMarkdownFencesAndRomanianNumbers() {
        ParsedReceipt receipt = reader.read("""
                ```json
                {"items": [{"name": "Brânză", "amount": "18,50 lei"}], "total": "18,50"}
                ```
                """);

        assertThat(receipt.items().getFirst().amount()).isEqualByComparingTo("18.50");
        assertThat(receipt.total()).isEqualByComparingTo("18.50");
    }

    @Test
    void turnsUnreadableFieldsIntoNull() {
        ParsedReceipt receipt = reader.read("""
                {"storeName": "", "date": "20.11.2026", "items": [{"name": "X", "amount": "abc"}], "total": null}
                """);

        assertThat(receipt.storeName()).isNull();
        assertThat(receipt.date()).isNull();
        assertThat(receipt.items().getFirst().amount()).isNull();
        assertThat(receipt.total()).isNull();
    }

    @Test
    void rejectsAnswersThatAreNotAJsonObject() {
        assertThatThrownBy(() -> reader.read("Sorry, I cannot read this receipt."))
                .isInstanceOf(UnreadableReceiptException.class);
        assertThatThrownBy(() -> reader.read("[1, 2, 3]")).isInstanceOf(UnreadableReceiptException.class);
        assertThatThrownBy(() -> reader.read("")).isInstanceOf(UnreadableReceiptException.class);
        assertThatThrownBy(() -> reader.read(null)).isInstanceOf(UnreadableReceiptException.class);
    }
}
