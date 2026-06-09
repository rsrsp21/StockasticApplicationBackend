package com.stockasticappbackend.service.marketai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stockasticappbackend.config.MarketAiProperties;
import com.stockasticappbackend.dto.marketai.MarketSnapshot;
import com.stockasticappbackend.dto.marketai.MarketStockDetails;
import com.stockasticappbackend.dto.marketai.MarketStockSummary;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.service.stock.StockService;
import com.stockasticappbackend.service.stockprice.StockPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * In-process replacement for the reactive market-data HTTP client. Builds the
 * market snapshot used for AI narratives from the monolith's own stock and
 * stock-price services instead of calling a remote market-data service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketAiDataService {

    private final StockPriceService stockPriceService;
    private final StockService stockService;
    private final MarketAiProperties properties;

    public MarketSnapshot loadSnapshot(String symbol) {
        List<MarketStockSummary> latestPrices = safeLatestPrices();
        List<MarketStockSummary> topGainers = safeMap(stockPriceService.getTopGainers(properties.getTopMoversLimit()));
        List<MarketStockSummary> topLosers = safeMap(stockPriceService.getTopLosers(properties.getTopMoversLimit()));

        MarketStockDetails stockDetails = null;
        if (symbol != null && !symbol.isBlank()) {
            stockDetails = loadStockDetails(symbol.trim().toUpperCase());
        }

        return MarketSnapshot.builder()
                .latestPrices(latestPrices)
                .topGainers(topGainers)
                .topLosers(topLosers)
                .stock(stockDetails)
                .build();
    }

    private List<MarketStockSummary> safeLatestPrices() {
        try {
            return stockPriceService.getAllLatestPrices().stream()
                    .limit(properties.getLatestPricesLimit())
                    .map(this::toSummary)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to load latest prices for AI snapshot: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<MarketStockSummary> safeMap(List<StockPriceResponse> prices) {
        try {
            return prices.stream().map(this::toSummary).toList();
        } catch (Exception ex) {
            log.warn("Failed to map mover data for AI snapshot: {}", ex.getMessage());
            return List.of();
        }
    }

    private MarketStockDetails loadStockDetails(String symbol) {
        try {
            StockResponse stock = stockService.getStockBySymbol(symbol);
            if (stock == null) {
                return null;
            }
            MarketStockDetails details = new MarketStockDetails();
            details.setStockId(stock.getStockId());
            details.setSymbol(stock.getSymbol());
            details.setName(stock.getName());
            details.setExchange(stock.getExchange());
            details.setSector(stock.getSector());
            details.setDescription(stock.getDescription());
            details.setCurrentPrice(stock.getCurrentPrice());
            details.setVolume(stock.getVolume());
            details.setAvgVolume(stock.getAvgVolume());
            details.setChangePercent(stock.getChangePercent());
            return details;
        } catch (Exception ex) {
            log.warn("Failed to load stock details for {} in AI snapshot: {}", symbol, ex.getMessage());
            return null;
        }
    }

    private MarketStockSummary toSummary(StockPriceResponse price) {
        MarketStockSummary summary = new MarketStockSummary();
        summary.setStockId(price.getStockId());
        summary.setSymbol(price.getSymbol());
        summary.setStockName(price.getStockName());
        summary.setPrice(price.getPrice());
        summary.setChangePercent(price.getChangePercent());
        summary.setVolume(price.getVolume());
        summary.setFinalVerdict(price.getFinalVerdict());
        summary.setPriceTime(price.getPriceTime());
        return summary;
    }
}
