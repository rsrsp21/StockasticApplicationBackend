package com.stockasticappbackend.exception;

/*
* This exception is thrown when a resource already exists.
*/
public class DuplicateResourceException extends RuntimeException {

    /**
     * Creates a new instance of {@code DuplicateResourceException}.
     *
     * @param message the detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
