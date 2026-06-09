package com.stockasticappbackend.dto.stockprice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for current stock price information.
 * Contains real-time or latest stored price data for a stock.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceResponse {

    /** The unique identifier of the price record. */
    private Long priceId;

    /** The unique identifier of the stock. */
    private Long stockId;

    /** The stock ticker symbol. */
    private String symbol;

    /** The full name of the stock. */
    private String stockName;

    /** The filename of the stock's logo image. */
    private String image;

    /** The current/latest price. */
    private BigDecimal price;

    /** The opening price of the day. */
    private BigDecimal openPrice;

    /** The previous day's closing price. */
    private BigDecimal previousClose;

    /** The day's high price. */
    private BigDecimal dayHigh;

    /** The day's low price. */
    private BigDecimal dayLow;

    /** The 52-week high price. */
    private BigDecimal fiftyTwoWeekHigh;

    /** The 52-week low price. */
    private BigDecimal fiftyTwoWeekLow;

    /** The trading volume. */
    private Long volume;

    /** 5-minute interval open price. */
    private BigDecimal intervalOpen;

    /** 5-minute interval high price. */
    private BigDecimal intervalHigh;

    /** 5-minute interval low price. */
    private BigDecimal intervalLow;

    /** 5-minute interval close price. */
    private BigDecimal intervalClose;

    /** 5-minute interval volume. */
    private Long intervalVolume;

    /** The percentage change from open. */
    private BigDecimal changePercent;

    /** The timestamp of the price data. */
    private LocalDateTime priceTime;

    // Technical Indicators
    private BigDecimal rsiValue;
    private String rsiVerdict;
    private BigDecimal macdValue;
    private BigDecimal macdSignal;
    private String macdVerdict;
    private String finalVerdict;
}
