package com.stockpulse.domain;

/**
 * Suggestion state machine: PENDING -> ACCEPTED | REJECTED (terminal).
 */
public enum SuggestionStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
