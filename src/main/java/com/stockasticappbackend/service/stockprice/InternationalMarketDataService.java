package com.stockasticappbackend.service.stockprice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.stockasticappbackend.dto.stockprice.StockPriceHistoryResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.yahoofinance.ChartData;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;
import com.stockasticappbackend.service.yahoofinance.YahooQuote;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternationalMarketDataService {

    private final StockRepository stockRepository;
    private final YahooFinanceService yahooFinanceService;

    public StockPriceResponse getLatestPrice(Long stockId) {
        Stock stock = getStockCached(stockId);
        YahooQuote quote = yahooFinanceService.getQuoteForResolvedSymbol(resolveYahooSymbol(stock), stock.getSymbol());

        if (quote == null) {
            throw new ResourceNotFoundException("Yahoo quote not found for symbol: " + stock.getSymbol());
        }

        BigDecimal baseline = quote.getPreviousClose() != null && quote.getPreviousClose().compareTo(BigDecimal.ZERO) > 0
                ? quote.getPreviousClose()
                : quote.getOpenPrice();
        BigDecimal changePercent = null;
        if (baseline != null && baseline.compareTo(BigDecimal.ZERO) > 0 && quote.getPrice() != null) {
            changePercent = quote.getPrice()
                    .subtract(baseline)
                    .divide(baseline, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return StockPriceResponse.builder()
                .stockId(stock.getStockId())
                .symbol(stock.getSymbol())
                .stockName(stock.getName())
                .image(stock.getImage())
                .price(quote.getPrice())
                .openPrice(quote.getOpenPrice())
                .previousClose(quote.getPreviousClose())
                .dayHigh(quote.getDayHigh())
                .dayLow(quote.getDayLow())
                .fiftyTwoWeekHigh(quote.getFiftyTwoWeekHigh())
                .fiftyTwoWeekLow(quote.getFiftyTwoWeekLow())
                .volume(quote.getVolume())
                .intervalOpen(quote.getIntervalOpen())
                .intervalHigh(quote.getIntervalHigh())
                .intervalLow(quote.getIntervalLow())
                .intervalClose(quote.getIntervalClose())
                .intervalVolume(quote.getIntervalVolume())
                .changePercent(changePercent)
                .priceTime(quote.getPriceTime())
                .build();
    }

    public StockPriceHistoryResponse getIntradayHistory(Long stockId, String range) {
        Stock stock = getStockCached(stockId);
        String yahooSymbol = resolveYahooSymbol(stock);
        boolean isOneDayRequest = range == null || range.isBlank() || "1d".equalsIgnoreCase(range);

        List<YahooQuote> quotes = (isOneDayRequest
                ? yahooFinanceService.getIntradayQuotesForResolvedSymbol(yahooSymbol, stock.getSymbol())
                : yahooFinanceService.getExtendedIntradayQuotesForResolvedSymbol(yahooSymbol, stock.getSymbol(), range))
                .stream()
                .sorted(Comparator.comparing(YahooQuote::getPriceTime))
                .toList();

        if (isOneDayRequest && quotes.isEmpty()) {
            quotes = getLatestCompletedSessionQuotes(yahooSymbol, stock.getSymbol());
        }

        List<StockPriceHistoryResponse.PricePoint> priceHistory = quotes.stream()
                .map(quote -> StockPriceHistoryResponse.PricePoint.builder()
                        .price(quote.getPrice())
                        .previousClose(quote.getPreviousClose())
                        .openPrice(quote.getOpenPrice())
                        .dayHigh(quote.getDayHigh())
                        .dayLow(quote.getDayLow())
                        .volume(quote.getVolume())
                        .intervalOpen(quote.getIntervalOpen())
                        .intervalHigh(quote.getIntervalHigh())
                        .intervalLow(quote.getIntervalLow())
                        .intervalClose(quote.getIntervalClose())
                        .intervalVolume(quote.getIntervalVolume())
                        .priceTime(quote.getPriceTime())
                        .build())
                .toList();

        return StockPriceHistoryResponse.builder()
                .stockId(stock.getStockId())
                .symbol(stock.getSymbol())
                .stockName(stock.getName())
                .priceHistory(priceHistory)
                .build();
    }

    public ChartData getHistoricalChart(Long stockId, String range) {
        Stock stock = getStockCached(stockId);
        return yahooFinanceService.getHistoricalDataForResolvedSymbol(resolveYahooSymbol(stock), stock.getSymbol(), range);
    }

    private List<YahooQuote> getLatestCompletedSessionQuotes(String yahooSymbol, String displaySymbol) {
        List<YahooQuote> extendedQuotes = yahooFinanceService
                .getExtendedIntradayQuotesForResolvedSymbol(yahooSymbol, displaySymbol, "5d")
                .stream()
                .sorted(Comparator.comparing(YahooQuote::getPriceTime))
                .toList();

        if (extendedQuotes.isEmpty()) {
            return List.of();
        }

        // Get all unique trading dates
        Set<LocalDate> tradingDates = extendedQuotes.stream()
                .map(quote -> quote.getPriceTime().toLocalDate())
                .collect(Collectors.toSet());

        if (tradingDates.isEmpty()) {
            return List.of();
        }

        // Sort dates in descending order to get the most recent trading dates
        List<LocalDate> sortedDates = tradingDates.stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        // During non-market hours, show previous complete trading day's data
        // If we have multiple dates, use the second-most recent (previous day)
        // Otherwise use the most recent available date
        LocalDate selectedDate = sortedDates.size() > 1 ? sortedDates.get(1) : sortedDates.get(0);

        return extendedQuotes.stream()
                .filter(quote -> quote.getPriceTime() != null && quote.getPriceTime().toLocalDate().equals(selectedDate))
                .toList();
    }

    @Cacheable(value = "stockById", key = "#stockId")
    public Stock getStockCached(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with id: " + stockId));
    }

    private String resolveYahooSymbol(Stock stock) {
        String symbol = stock.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            throw new ResourceNotFoundException("Stock symbol missing for stock id: " + stock.getStockId());
        }
        if (symbol.contains(".")) {
            return symbol;
        }

        String exchange = stock.getExchange() == null ? "" : stock.getExchange().trim().toUpperCase();
        return switch (exchange) {
            case "NSE" -> symbol + ".NS";
            case "BSE" -> symbol + ".BO";
            default -> symbol;
        };
    }
}
