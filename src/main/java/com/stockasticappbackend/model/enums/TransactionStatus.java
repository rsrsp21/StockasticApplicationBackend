package com.stockasticappbackend.model.enums;

/**
 * Enum representing the status of a wallet transaction.
 */
public enum TransactionStatus {
    /** Transaction is being processed. */
    PENDING,
    
    /** Transaction completed successfully. */
    SUCCESS,
    
    /** Transaction failed. */
    FAILED
}
