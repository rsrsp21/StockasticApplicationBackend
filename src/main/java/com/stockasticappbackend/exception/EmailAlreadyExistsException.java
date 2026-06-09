package com.stockasticappbackend.exception;

/**
 * Exception thrown when attempting to register with an email that already
 * exists.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new EmailAlreadyExistsException with the specified message.
     *
     * @param message The detail message.
     */
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
