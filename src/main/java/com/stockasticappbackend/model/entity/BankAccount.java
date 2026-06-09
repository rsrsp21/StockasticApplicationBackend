package com.stockasticappbackend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a linked bank account for withdrawals.
 * A user can link multiple bank accounts.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    /** Unique identifier for the bank account. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this bank account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Bank name (e.g., "HDFC Bank"). */
    @Column(nullable = false, length = 100)
    private String bankName;

    /** Account number (stored securely, displayed masked). */
    @Column(nullable = false, length = 50)
    private String accountNumber;

    /** IFSC code. */
    @Column(nullable = false, length = 20)
    private String ifscCode;

    /** Account holder name as per bank records. */
    @Column(nullable = false, length = 100)
    private String holderName;

    /** Whether the bank account has been verified (e.g., penny-drop). */
    @Column(nullable = false)
    private Boolean isVerified = false;

    /** Whether this is the user's primary bank account for transactions. */
    @Column(nullable = false)
    private Boolean isPrimary = false;
}
