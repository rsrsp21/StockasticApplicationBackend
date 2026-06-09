package com.stockasticappbackend.exception;

/**
 * Exception thrown when user has insufficient stock holdings to sell.
 */
public class InsufficientHoldingsException extends RuntimeException {

    public InsufficientHoldingsException(String message) {
        super(message);
    }
}
