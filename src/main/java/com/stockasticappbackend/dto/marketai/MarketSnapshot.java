package com.stockasticappbackend.dto.marketai;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketSnapshot {

    List<MarketStockSummary> latestPrices;
    List<MarketStockSummary> topGainers;
    List<MarketStockSummary> topLosers;
    MarketStockDetails stock;
}
