package com.stockasticappbackend.dto.marketai;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MarketStockSummary {

    private Long stockId;
    private String symbol;
    private String stockName;
    private BigDecimal price;
    private BigDecimal changePercent;
    private Long volume;
    private String finalVerdict;
    private LocalDateTime priceTime;
}
