package com.stockasticappbackend.model.enums;

/**
 * Enum representing market trading sessions.
 */
public enum MarketSession {
    /** Before market opens (before 9:15 AM IST). */
    PRE_MARKET,
    /** Market is open (9:15 AM - 3:30 PM IST). */
    MARKET_HOURS,
    /** After market closes (after 3:30 PM IST). */
    AFTER_MARKET
}
