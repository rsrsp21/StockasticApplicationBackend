package com.stockasticappbackend.exception;

/**
 * Exception thrown when a bank account is not found.
 */
public class BankAccountNotFoundException extends RuntimeException {

    /**
     * Constructs a new BankAccountNotFoundException with the specified message.
     *
     * @param message The detail message.
     */
    public BankAccountNotFoundException(String message) {
        super(message);
    }
}
