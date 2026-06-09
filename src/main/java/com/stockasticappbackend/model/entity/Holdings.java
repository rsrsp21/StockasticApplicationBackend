package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a user's stock holdings.
 * Tracks the quantity of each stock owned by a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "stock_id"})
})
public class Holdings {

    /** Unique identifier for the holding. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holdingId;

    /** The user who owns this holding. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** The stock being held. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    /** Total quantity of shares owned. */
    @Column(nullable = false)
    private Integer quantity;

    /** Quantity locked in pending sell orders. */
    @Column(nullable = false)
    @Builder.Default
    private Integer lockedQuantity = 0;

    /** Average buy price for P&L calculation. */
    @Column(name = "avg_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal averagePrice;

    /** Total realized P&L from selling this stock. */
    @Column(name = "total_realized_pnl", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRealizedPnl = BigDecimal.ZERO;

    /** Timestamp when the holding was last updated. */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Gets the available quantity (not locked in orders).
     *
     * @return Available quantity for selling.
     */
    public Integer getAvailableQuantity() {
        return quantity - lockedQuantity;
    }
}