package com.stockasticappbackend.dto.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for stock information.
 * Contains all publicly visible stock data fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {

    /** The unique identifier of the stock. */
    private Long stockId;

    /** The stock ticker symbol. */
    private String symbol;

    /** The full name of the company/stock. */
    private String name;

    /** The exchange where the stock is traded. */
    private String exchange;

    /** The industry sector. */
    private String sector;

    /** A brief description of the stock. */
    private String description;

    /** The filename of the stock's logo image. */
    private String image;

    /** Whether the stock is active and visible. */
    private Boolean isActive;

    /** The timestamp when the stock was created. */
    private LocalDateTime createdAt;

    /** The current price of the stock. */
    private BigDecimal currentPrice;

    /** The trading volume. */
    private Long volume;

    /** The average trading volume (per candle) for the day. */
    private Long avgVolume;

    /** The percentage change (placeholder if not calculated). */
    private BigDecimal changePercent;
}