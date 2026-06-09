package com.stockasticappbackend.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.stockasticappbackend.model.entity.AutoSellRule;
import com.stockasticappbackend.model.entity.PriceAlert;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

/**
 * Central Caffeine cache configuration.
 * Defines both Spring @Cacheable caches (via CacheManager) and
 * programmatic Cache<> beans for services that need direct cache manipulation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Spring @Cacheable / @CacheEvict caches

    @Bean
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("allStocks",       600, 50),    // 10 min
                buildCache("stockById",       600, 1000),  // 10 min
                buildCache("stockBySymbol",   600, 1000),  // 10 min
                buildCache("latestPrices",    20,  200),   // 20 sec
                buildCache("latestPriceById", 20,  2000),  // 20 sec
                buildCache("dashboardStats",  300, 20),    // 5 min
                buildCache("marketStatus",    15,  10),    // 15 sec
                buildCache("mostTraded",      180, 50),    // 3 min
                buildCache("userSips",        180, 2000)   // 3 min
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int ttlSeconds, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }

    // Programmatic caches (injected into services directly)

    @Bean
    Cache<Long, List<PriceAlert>> priceAlertCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .recordStats()
                .build();
    }

    @Bean
    Cache<Long, List<AutoSellRule>> autoSellRuleCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .recordStats()
                .build();
    }

    @Bean
    Cache<Long, List<BigDecimal>> indicatorWindowCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofHours(6))
                .recordStats()
                .build();
    }

    @Bean
    Cache<Long, Boolean> internationalStockCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                // No TTL — stock market type never changes at runtime
                .recordStats()
                .build();
    }

    // Register programmatic caches with Micrometer for /actuator/metrics

    @Bean
    MeterBinder bindCacheMetrics(
            Cache<Long, List<PriceAlert>> priceAlertCache,
            Cache<Long, List<AutoSellRule>> autoSellRuleCache,
            Cache<Long, List<BigDecimal>> indicatorWindowCache,
            Cache<Long, Boolean> internationalStockCache) {
        return registry -> {
            CaffeineCacheMetrics.monitor(
                    registry, priceAlertCache, "priceAlertCache");
            CaffeineCacheMetrics.monitor(
                    registry, autoSellRuleCache, "autoSellRuleCache");
            CaffeineCacheMetrics.monitor(
                    registry, indicatorWindowCache, "indicatorWindowCache");
            CaffeineCacheMetrics.monitor(
                    registry, internationalStockCache, "internationalStockCache");
        };
    }
}
