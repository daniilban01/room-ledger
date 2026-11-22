package com.roomledger.activity;

public enum ActivityType {
    GROUP_CREATED,
    GROUP_RENAMED,
    GROUP_DELETED,
    MEMBER_JOINED,
    EXPENSE_ADDED,
    EXPENSE_EDITED,
    EXPENSE_DELETED,
    /** The payer said "I paid" (PENDING). */
    SETTLEMENT_MARKED_PAID,
    /** The receiver recorded a payment directly (CONFIRMED immediately). */
    SETTLEMENT_RECORDED,
    SETTLEMENT_CONFIRMED,
    SETTLEMENT_REJECTED
}
