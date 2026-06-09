package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.stockasticappbackend.model.enums.TransactionStatus;
import com.stockasticappbackend.model.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity representing a wallet transaction.
 * Records the history of all money movements (Deposits, Withdrawals, Trade Deductions).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    /** Unique identifier for the transaction. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    /** The wallet this transaction belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** Transaction amount. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Type of transaction (CREDIT or DEBIT). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Status of the transaction. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /** External reference ID (e.g., Payment Gateway ID, Order ID). */
    @Column(unique = true)
    private String referenceId;

    /** User-facing description (e.g., "Added via UPI"). */
    private String description;

    /** Timestamp when the transaction was created. */
    @CreationTimestamp
    private LocalDateTime createdAt;
}
