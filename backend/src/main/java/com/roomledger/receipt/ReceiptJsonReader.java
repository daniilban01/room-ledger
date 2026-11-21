package com.roomledger.receipt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns the model's text answer into a ParsedReceipt. Strict about the overall shape (must be a JSON
 * object), lenient about details: a bad date or price becomes null and the validator decides what to do.
 */
public class ReceiptJsonReader {

    // BigDecimal for floats: "12.30" stays exactly 12.30 instead of going through a double.
    private final ObjectReader reader = new ObjectMapper().reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    public ParsedReceipt read(String modelOutput) {
        if (modelOutput == null || modelOutput.isBlank()) {
            throw new UnreadableReceiptException("The model returned an empty answer");
        }

        JsonNode root;
        try {
            root = reader.readTree(stripCodeFences(modelOutput));
        } catch (JsonProcessingException e) {
            throw new UnreadableReceiptException("The model did not return valid JSON", e);
        }
        if (root == null || !root.isObject()) {
            throw new UnreadableReceiptException("The model did not return a JSON object");
        }

        List<ParsedReceipt.Item> items = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            if (item.isObject()) {
                items.add(new ParsedReceipt.Item(text(item.get("name")), decimal(item.get("amount"))));
            }
        }
        return new ParsedReceipt(text(root.get("storeName")), date(root.get("date")), items, decimal(root.get("total")));
    }

    /** Models sometimes wrap JSON in ```json ... ``` despite being told not to. */
    private static String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static String text(JsonNode node) {
        return node == null || !node.isTextual() || node.asText().isBlank() ? null : node.asText().trim();
    }

    private static LocalDate date(JsonNode node) {
        String value = text(node);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Accepts 12.5 as well as "12,50" or "12.50 lei", which models produce now and then. */
    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            String cleaned = node.asText().replaceAll("(?i)lei|ron|\\s", "").replace(',', '.');
            try {
                return new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
