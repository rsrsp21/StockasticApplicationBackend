package com.stockasticappbackend.dto.marketai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StockComparisonResult {

    String primarySymbol;
    String comparisonSymbol;
    BigDecimal primaryPrice;
    BigDecimal comparisonPrice;
    Long primaryVolume;
    Long comparisonVolume;
    LocalDate tradingDate;
    LocalTime primaryRequestedTime;
    LocalTime primaryMatchedTime;
    LocalTime comparisonRequestedTime;
    LocalTime comparisonMatchedTime;
    BigDecimal priceDifference;
    BigDecimal percentageDifference;
}
