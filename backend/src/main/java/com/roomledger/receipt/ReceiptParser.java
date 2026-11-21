package com.roomledger.receipt;

/**
 * Reads a receipt photo. Behind an interface so the rest of the code does not care whether
 * the answer comes from a real LLM (production) or a canned response (tests, local development).
 */
public interface ReceiptParser {

    /**
     * @param image     the raw image bytes (already checked to be JPEG, PNG or WebP)
     * @param mediaType e.g. "image/jpeg"
     * @throws UnreadableReceiptException if the answer cannot be understood
     * @throws ReceiptProviderException   if the provider cannot be reached or fails
     */
    ParsedReceipt parse(byte[] image, String mediaType);
}
