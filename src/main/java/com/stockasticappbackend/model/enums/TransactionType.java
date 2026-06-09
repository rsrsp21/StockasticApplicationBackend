package com.stockasticappbackend.model.enums;

/**
 * Enum representing the type of wallet transaction.
 */
public enum TransactionType {
    /** Money added to wallet (deposit, refund). */
    CREDIT,
    
    /** Money removed from wallet (withdrawal, purchase). */
    DEBIT
}
