package com.stockasticappbackend.model.enums;

/**
 * Enum representing the status of an order.
 */
public enum OrderStatus {
    /** Order is pending execution (AMO or limit order waiting). */
    PENDING,
    /** Order is partially filled (some quantity executed). */
    PARTIALLY_FILLED,
    /** Order has been fully executed. */
    FILLED,
    /** Order was cancelled by user. */
    CANCELLED,
    /** Order was rejected (validation failed). */
    REJECTED
}
