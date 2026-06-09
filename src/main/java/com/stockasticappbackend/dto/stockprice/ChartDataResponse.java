package com.stockasticappbackend.dto.stockprice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for historical chart data.
 * Used for 1W, 1M, 3M, 1Y, and 3Y chart endpoints.
 * 
 * Contains OHLCV (Open, High, Low, Close, Volume) data points.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataResponse {

    /** The stock ticker symbol. */
    private String symbol;

    /** The time range (e.g., "1W", "1M", "3M", "1Y", "3Y"). */
    private String range;

    /** The data interval (e.g., "5m", "1h", "1d"). */
    private String interval;

    /** The list of chart data points. */
    private List<ChartPoint> data;

    /**
     * Represents a single OHLCV data point on the chart.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartPoint {

        /** The timestamp of this data point. */
        private LocalDateTime time;

        /** The opening price. */
        private BigDecimal open;

        /** The highest price during the period. */
        private BigDecimal high;

        /** The lowest price during the period. */
        private BigDecimal low;

        /** The closing price. */
        private BigDecimal close;

        /** The trading volume. */
        private Long volume;
    }
}
