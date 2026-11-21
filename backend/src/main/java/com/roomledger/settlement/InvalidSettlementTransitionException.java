package com.roomledger.settlement;

/** Thrown by the Settlement state machine; the service turns it into a 409 Conflict. */
public class InvalidSettlementTransitionException extends RuntimeException {

    public InvalidSettlementTransitionException(SettlementStatus from, SettlementStatus to) {
        super("A settlement can go from %s to %s only while it is PENDING".formatted(from, to));
    }
}
