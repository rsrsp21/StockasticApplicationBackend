package com.stockasticappbackend.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing technical indicators for a stock.
 * Stores RSI, MACD and overall trading verdicts.
 */
@Entity
@Table(name = "stock_indicator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, unique = true)
    private Stock stock;

    @Column(name = "rsi_value", precision = 10, scale = 2)
    private BigDecimal rsiValue;

    @Column(name = "rsi_verdict")
    private String rsiVerdict;

    @Column(name = "macd_value", precision = 10, scale = 2)
    private BigDecimal macdValue;

    @Column(name = "macd_signal", precision = 10, scale = 2)
    private BigDecimal macdSignal;

    @Column(name = "macd_verdict")
    private String macdVerdict;

    @Column(name = "final_verdict")
    private String finalVerdict;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
