package com.stockasticappbackend.dto.stockprice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for stock price history.
 * Contains a list of historical price points for a specific stock
 * within a requested time range.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceHistoryResponse {

    /** The unique identifier of the stock. */
    private Long stockId;

    /** The stock ticker symbol. */
    private String symbol;

    /** The full name of the stock. */
    private String stockName;

    /** The list of historical price points. */
    private List<PricePoint> priceHistory;

    /**
     * Represents a single historical price point.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricePoint {

        /** The current/closing price. */
        private BigDecimal price;

        /** The previous day's closing price (for change calculation). */
        private BigDecimal previousClose;

        /** The opening price. */
        private BigDecimal openPrice;

        /** The day's high price. */
        private BigDecimal dayHigh;

        /** The day's low price. */
        private BigDecimal dayLow;

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

        /** The timestamp of this price point. */
        private LocalDateTime priceTime;
    }
}
