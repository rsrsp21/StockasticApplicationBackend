package com.stockasticappbackend.exception;

/**
 * Exception thrown when a wallet is not found for a user.
 */
public class WalletNotFoundException extends RuntimeException {

    /**
     * Constructs a new WalletNotFoundException with the specified message.
     *
     * @param message The detail message.
     */
    public WalletNotFoundException(String message) {
        super(message);
    }
}
