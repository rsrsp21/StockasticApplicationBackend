package com.stockasticappbackend.exception;

/**
 * Exception thrown when OTP verification fails.
 */
public class InvalidOtpException extends RuntimeException {

    /**
     * Constructs a new InvalidOtpException with the specified message.
     *
     * @param message The detail message.
     */
    public InvalidOtpException(String message) {
        super(message);
    }
}
