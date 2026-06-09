package com.stockasticappbackend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import com.stockasticappbackend.model.entity.ActivityLog;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.service.activity.ActivityLogInternalService;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @org.springframework.context.annotation.Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean
    @Primary
    public ActivityLogInternalService activityLogInternalServiceStub(ActivityLogRepository repo) {
        return new ActivityLogInternalService(repo) {
            @Override
            public void save(ActivityLog logEntry) {
                // No-op stub to prevent database writes during tests
            }
        };
    }
    @Bean
    @Primary
    public com.stockasticappbackend.service.stockprice.StockPriceService stockPriceServiceStub() {
        return new com.stockasticappbackend.service.stockprice.StockPriceService() {
            @Override
            public com.stockasticappbackend.dto.stockprice.StockPriceResponse getLatestPrice(Long stockId) {
                return createDummyResponse();
            }

            @Override
            public com.stockasticappbackend.dto.stockprice.StockPriceResponse getLatestPriceBySymbol(String symbol) {
                return createDummyResponse();
            }

            @Override
            public java.util.List<com.stockasticappbackend.dto.stockprice.StockPriceResponse> getAllLatestPrices() {
                return java.util.Collections.emptyList();
            }

            @Override
            public com.stockasticappbackend.dto.stockprice.StockPriceHistoryResponse getPriceHistory(Long stockId,
                    java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
                return null;
            }

            @Override
            public com.stockasticappbackend.dto.stockprice.IndicatorSeriesResponse getIndicatorSeries(Long stockId,
                    String range) {
                return com.stockasticappbackend.dto.stockprice.IndicatorSeriesResponse.builder()
                        .stockId(stockId)
                        .range(range)
                        .points(java.util.Collections.emptyList())
                        .build();
            }

            @Override
            public void fetchAndStorePrices() {
            }

            @Override
            public void reconcileLatestCandles() {
            }

            @Override
            public void forceFetchPrices() {
            }

            @Override
            public void clearPreviousDayPrices() {
            }

            @Override
            public java.util.List<com.stockasticappbackend.dto.stockprice.StockPriceResponse> getTopGainers(int limit) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<com.stockasticappbackend.dto.stockprice.StockPriceResponse> getTopLosers(int limit) {
                return java.util.Collections.emptyList();
            }

            @Override
            public void cleanupOldPrices(int daysToKeep) {
            }

            private com.stockasticappbackend.dto.stockprice.StockPriceResponse createDummyResponse() {
                return com.stockasticappbackend.dto.stockprice.StockPriceResponse.builder()
                        .price(java.math.BigDecimal.valueOf(1500.00))
                        .openPrice(java.math.BigDecimal.valueOf(1450.00))
                        .previousClose(java.math.BigDecimal.valueOf(1400.00))
                        .changePercent(java.math.BigDecimal.TEN)
                        .dayHigh(java.math.BigDecimal.valueOf(1550.00))
                        .dayLow(java.math.BigDecimal.valueOf(1420.00))
                        .volume(1000L)
                        .build();
            }
        };
    }
}
