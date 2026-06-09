package com.stockasticappbackend.dto.stockprice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chart indicator series.
 * Contains timestamp-aligned RSI and MACD values for plotting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorSeriesResponse {

    /** Stock identifier. */
    private Long stockId;

    /** Stock symbol. */
    private String symbol;

    /** Requested range (e.g. 1D). */
    private String range;

    /** Indicator points aligned to chart timestamps. */
    private List<IndicatorPoint> points;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndicatorPoint {
        private LocalDateTime time;
        private BigDecimal rsi;
        private BigDecimal macd;
        private BigDecimal signal;
        private BigDecimal histogram;
    }
}
