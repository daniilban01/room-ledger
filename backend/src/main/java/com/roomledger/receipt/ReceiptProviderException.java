package com.roomledger.receipt;

/** The LLM provider was unreachable, timed out or returned an error (the service turns this into 502). */
public class ReceiptProviderException extends RuntimeException {

    public ReceiptProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
