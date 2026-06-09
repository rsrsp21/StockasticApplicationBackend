package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a user's wallet.
 * Each user has exactly one wallet that tracks available and locked balances.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "wallets")
public class Wallet {

    /** Unique identifier for the wallet. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    /** The user who owns this wallet. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId", nullable = false, unique = true)
    private AppUser user;

    /** Funds available for withdrawal or new trades. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /** Funds locked in pending buy orders. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    /** Currency code (default: INR). */
    @Column(length = 3)
    private String currency = "INR";

    /** Timestamp when the wallet was created. */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Timestamp when the wallet was last updated. */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** List of transactions associated with this wallet. */

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WalletTransaction> transactions = new ArrayList<>();

    /**
     * Calculates the total wallet value (available + locked).
     *
     * @return Total wallet balance.
     */
    public BigDecimal getTotalBalance() {
        return availableBalance.add(lockedBalance);
    }
}
