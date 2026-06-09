package com.stockasticappbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link MarketAiProperties} for the market-AI feature.
 */
@Configuration
@EnableConfigurationProperties(MarketAiProperties.class)
public class MarketAiConfig {
}
