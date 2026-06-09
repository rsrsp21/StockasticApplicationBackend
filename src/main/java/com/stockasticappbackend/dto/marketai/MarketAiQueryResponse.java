package com.stockasticappbackend.dto.marketai;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketAiQueryResponse {

    String model;
    String answer;
    String symbol;
    MarketSnapshot snapshot;
}
