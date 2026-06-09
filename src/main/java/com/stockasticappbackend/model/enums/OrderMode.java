package com.stockasticappbackend.model.enums;

/**
 * Enum representing the order execution mode.
 */
public enum OrderMode {
    /** Market order - executes at current market price. */
    MARKET,
    /** Limit order - executes only at specified price or better. */
    LIMIT
}
