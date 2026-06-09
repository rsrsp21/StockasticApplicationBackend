package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.stockasticappbackend.model.enums.MarketSession;
import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderStatus;
import com.stockasticappbackend.model.enums.OrderType;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a stock order (buy or sell).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    /** Unique identifier for the order. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    /** The user who placed this order. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** The stock being traded. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    /** Type of order (BUY or SELL). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    /** Mode of order (MARKET or LIMIT). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderMode orderMode;

    /** Current status of the order. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** Market session when order was placed. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketSession marketSession;

    /** Whether this is an After Market Order (AMO). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAmo = false;

    /** Quantity of shares to trade. */
    @Column(nullable = false)
    private Integer quantity;

    /** Quantity filled so far (for partial fills). */
    @Column(nullable = false)
    @Builder.Default
    private Integer filledQuantity = 0;

    /** Price per share (limit price or executed price). */
    @Column(precision = 15, scale = 4)
    private BigDecimal price;

    /** Average price at which order was filled. */
    @Column(precision = 15, scale = 4)
    private BigDecimal averageFilledPrice;

    /** Total order value (quantity × price). */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /** Realized P&L for SELL orders. */
    @Column(precision = 15, scale = 2)
    private BigDecimal realizedPnl;

    /** Amount blocked in wallet for this order (buy orders). */
    @Column(precision = 15, scale = 2)
    private BigDecimal blockedAmount;

    /** Amount funded from locked balance (unsettled funds). */
    @Column(precision = 15, scale = 2)
    private BigDecimal fundedFromLocked;

    /** Brokerage fee charged for this order. */
    @Column(precision = 15, scale = 2)
    private BigDecimal brokerage;

    /** Quantity blocked in holdings for this order (sell orders). */
    private Integer blockedQuantity;

    /** Timestamp when the order was created. */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Timestamp when the order was executed/filled. */
    private LocalDateTime executedAt;

    /** Reason for rejection if REJECTED. */
    private String rejectionReason;

    /** Whether funds from this SELL order have been settled (T+1 rule). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSettled = false;

    /** Timestamp when the sell order funds were settled. */
    private LocalDateTime settledAt;
}
