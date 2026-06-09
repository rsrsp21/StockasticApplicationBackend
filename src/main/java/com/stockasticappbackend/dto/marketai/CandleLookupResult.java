package com.stockasticappbackend.dto.marketai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CandleLookupResult {

    Long stockId;
    String symbol;
    String stockName;
    LocalDate tradingDate;
    LocalTime requestedTime;
    LocalDateTime matchedCandleTime;
    BigDecimal price;
    Long volume;
}
