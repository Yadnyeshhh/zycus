package com.stockpulse.domain;

/**
 * Product lifecycle: ACTIVE -> PRICE_REVIEW_PENDING -> ACTIVE (OUT_OF_STOCK when stock = 0).
 */
public enum ProductStatus {
    ACTIVE,
    PRICE_REVIEW_PENDING,
    OUT_OF_STOCK
}
