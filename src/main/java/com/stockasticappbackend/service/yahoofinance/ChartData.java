package com.stockasticappbackend.service.yahoofinance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for historical chart data from Yahoo Finance.
 * Contains OHLCV data points for a specified time range.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {
    private String symbol;
    private String range;
    private String interval;
    private List<DataPoint> dataPoints;

    /**
     * Single OHLCV data point for chart display.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPoint {
        private LocalDateTime time;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
    }
}
