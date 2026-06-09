package com.stockasticappbackend.exception;

/**
 * Exception thrown when a withdrawal amount exceeds the available balance.
 */
public class InsufficientFundsException extends RuntimeException {

    /**
     * Constructs a new InsufficientFundsException with the specified message.
     *
     * @param message The detail message.
     */
    public InsufficientFundsException(String message) {
        super(message);
    }
}
