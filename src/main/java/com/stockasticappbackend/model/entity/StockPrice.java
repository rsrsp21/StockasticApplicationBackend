package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a stock price record.
 * Stores historical and real-time price data for stocks including
 * OHLC (Open, High, Low, Close) prices and volume.
 */
@Entity
@Table(name = "stock_price", indexes = {
        @Index(name = "idx_stock_price_stock_time", columnList = "stock_id, price_time"),
        @Index(name = "idx_stock_price_time", columnList = "price_time")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "stock_id", "price_time" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPrice {

    /** The unique identifier of the price record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    private Long priceId;

    /** The stock this price belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    /** The previous day's closing price. */
    @Column(name = "previous_close", precision = 15, scale = 4)
    private BigDecimal previousClose;

    /** The day's high price. */
    @Column(name = "day_high", precision = 15, scale = 4)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 15, scale = 4)
    private BigDecimal dayLow;

    /** 52-week high price. */
    @Column(name = "fifty_two_week_high", precision = 15, scale = 4)
    private BigDecimal fiftyTwoWeekHigh;

    /** 52-week low price. */
    @Column(name = "fifty_two_week_low", precision = 15, scale = 4)
    private BigDecimal fiftyTwoWeekLow;

    /** 5-minute interval open price. */
    @Column(name = "interval_open", precision = 15, scale = 4)
    private BigDecimal intervalOpen;

    /** 5-minute interval high price. */
    @Column(name = "interval_high", precision = 15, scale = 4)
    private BigDecimal intervalHigh;

    /** 5-minute interval low price. */
    @Column(name = "interval_low", precision = 15, scale = 4)
    private BigDecimal intervalLow;

    /** 5-minute interval close price. */
    @Column(name = "interval_close", precision = 15, scale = 4)
    private BigDecimal intervalClose;

    /** 5-minute interval volume. */
    @Column(name = "interval_volume")
    private Long intervalVolume;

    /**
     * Backward-compatible aliases used by legacy code/tests.
     * Not persisted; synced to interval fields in lifecycle hooks.
     */
    @Transient
    private BigDecimal price;

    @Transient
    private BigDecimal openPrice;

    @Transient
    private Long volume;

    /** The timestamp of this price record. */
    @Column(name = "price_time", nullable = false)
    private LocalDateTime priceTime;

    public BigDecimal getPrice() {
        return intervalClose != null ? intervalClose : price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        if (price != null) {
            this.intervalClose = price;
        }
    }

    public BigDecimal getOpenPrice() {
        return intervalOpen != null ? intervalOpen : openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
        if (openPrice != null) {
            this.intervalOpen = openPrice;
        }
    }

    public Long getVolume() {
        return intervalVolume != null ? intervalVolume : volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
        if (volume != null) {
            this.intervalVolume = volume;
        }
    }

    @PrePersist
    @PreUpdate
    void syncLegacyToInterval() {
        if (intervalClose == null && price != null) intervalClose = price;
        if (intervalOpen == null && openPrice != null) intervalOpen = openPrice;
        if (intervalVolume == null && volume != null) intervalVolume = volume;
    }

    @PostLoad
    void syncIntervalToLegacy() {
        this.price = intervalClose;
        this.openPrice = intervalOpen;
        this.volume = intervalVolume;
    }
}
