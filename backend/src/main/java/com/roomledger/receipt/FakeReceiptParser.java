package com.roomledger.receipt;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Canned answer for tests and for running locally without an API key (profile "dev-fake").
 * Whatever image you upload, you get this receipt back.
 */
@Component
@Profile("dev-fake")
public class FakeReceiptParser implements ReceiptParser {

    @Override
    public ParsedReceipt parse(byte[] image, String mediaType) {
        return new ParsedReceipt(
                "Magazin Demo",
                LocalDate.of(2026, 11, 20),
                List.of(
                        new ParsedReceipt.Item("Lapte 1.5%", new BigDecimal("7.49")),
                        new ParsedReceipt.Item("Pâine albă", new BigDecimal("4.99")),
                        new ParsedReceipt.Item("Brânză telemea", new BigDecimal("18.50")),
                        new ParsedReceipt.Item("Mere", new BigDecimal("6.32"))),
                new BigDecimal("37.30"));
    }
}
