package com.roomledger.receipt;

/** The image was processed but no usable receipt data came out of it (the service turns this into 422). */
public class UnreadableReceiptException extends RuntimeException {

    public UnreadableReceiptException(String message) {
        super(message);
    }

    public UnreadableReceiptException(String message, Throwable cause) {
        super(message, cause);
    }
}
