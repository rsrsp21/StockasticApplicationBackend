package com.stockasticappbackend.service.yahoofinance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Yahoo Finance quote data.
 * Contains price information for a single stock at a point in time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YahooQuote {
    private String symbol;
    private BigDecimal price;
    private BigDecimal previousClose;
    private BigDecimal openPrice;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;
    private Long volume;
    private BigDecimal intervalOpen;
    private BigDecimal intervalHigh;
    private BigDecimal intervalLow;
    private BigDecimal intervalClose;
    private Long intervalVolume;
    private LocalDateTime priceTime;
}
