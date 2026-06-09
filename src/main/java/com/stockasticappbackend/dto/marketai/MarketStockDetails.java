package com.stockasticappbackend.dto.marketai;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MarketStockDetails {

    private Long stockId;
    private String symbol;
    private String name;
    private String exchange;
    private String sector;
    private String description;
    private BigDecimal currentPrice;
    private Long volume;
    private Long avgVolume;
    private BigDecimal changePercent;
}
