package com.roomledger.expense;

/** Thrown by SplitCalculator. Plain Java on purpose: the calculator knows nothing about HTTP. */
public class InvalidSplitException extends RuntimeException {

    public InvalidSplitException(String message) {
        super(message);
    }
}
