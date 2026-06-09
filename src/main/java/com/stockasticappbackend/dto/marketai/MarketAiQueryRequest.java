package com.stockasticappbackend.dto.marketai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarketAiQueryRequest {

    @NotBlank
    private String query;

    private String symbol;
}
