package com.stockasticappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.market-ai")
public class MarketAiProperties {

    @NotBlank
    private String defaultModel = "gemini-2.5-flash-lite";

    @Min(1)
    private int topMoversLimit = 5;

    @Min(1)
    private int latestPricesLimit = 12;
}
