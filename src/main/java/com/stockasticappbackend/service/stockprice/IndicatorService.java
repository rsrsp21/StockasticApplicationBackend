package com.stockasticappbackend.service.stockprice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockIndicator;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.repository.StockIndicatorRepository;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;
import com.stockasticappbackend.service.yahoofinance.YahooQuote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndicatorService {

    private final StockIndicatorRepository indicatorRepository;
    private final StockPriceRepository stockPriceRepository;
    private final YahooFinanceService yahooFinanceService;
    private final MarketHoursService marketHoursService;
    
    // In-memory sliding window: Stock ID -> List of last 50 close prices
    private final Cache<Long, List<BigDecimal>> indicatorWindowCache;

    private static final int WINDOW_SIZE = 50;
    private static final int RSI_PERIOD = 14;
    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;

    /**
     * Initializes indicators for a stock on server startup.
     */
    @Transactional
    public void initializeIndicators(Stock stock) {
        log.info("Initializing indicators for stock: {}", stock.getSymbol());
        
        // 1. Load today's 5-minute closes from DB
        LocalDateTime startOfToday = marketHoursService.getCurrentISTTime().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);
        
        List<StockPrice> todayPrices = stockPriceRepository.findPriceHistoryByStockId(
                stock.getStockId(), startOfToday, endOfToday);
        
        List<BigDecimal> closes = new ArrayList<>();
        for (StockPrice sp : todayPrices) {
            closes.add(sp.getIntervalClose());
        }
        
        log.info("Stock {} has {} today's prices in DB", stock.getSymbol(), closes.size());
        
        // 2 & 3. If count < 50, fetch missing from Yahoo
        if (closes.size() < WINDOW_SIZE) {
            int missing = WINDOW_SIZE - closes.size();
            log.info("Fetching {} missing prices for {} from Yahoo", missing, stock.getSymbol());
            
            // Fetch 5 days to be safe, then find candles immediately before today's first candle
            List<YahooQuote> history = yahooFinanceService.getExtendedIntradayQuotes(stock.getSymbol(), "5d");
            
            if (history.isEmpty()) {
                log.warn("Could not fetch historical intraday data for {}", stock.getSymbol());
                if (!closes.isEmpty()) {
                    initializeWindow(stock.getStockId(), closes);
                }
                return;
            }
            
            // Filter out today's candles from history to get only "previous" candles
            // AND ensure they are immediately before today's first candle
            List<BigDecimal> previousCloses = new ArrayList<>();
            for (int i = history.size() - 1; i >= 0 && previousCloses.size() < missing; i--) {
                YahooQuote quote = history.get(i);
                if (quote.getPriceTime().isBefore(startOfToday)) {
                    previousCloses.add(quote.getPrice());
                }
            }
            Collections.reverse(previousCloses);
            
            List<BigDecimal> merged = new ArrayList<>(previousCloses);
            merged.addAll(closes);
            
            if (merged.size() >= WINDOW_SIZE) {
                initializeWindow(stock.getStockId(), merged);
                calculateAndSave(stock, indicatorWindowCache.getIfPresent(stock.getStockId()));
            } else {
                log.warn("Still insufficient data for {} after fetching history: {}/{}", 
                        stock.getSymbol(), merged.size(), WINDOW_SIZE);
                initializeWindow(stock.getStockId(), merged);
            }
        } else {
            // Already have 50 or more for today
            initializeWindow(stock.getStockId(), closes);
            calculateAndSave(stock, indicatorWindowCache.getIfPresent(stock.getStockId()));
        }
    }

    /**
     * Initializes the sliding window for a stock.
     */
    public void initializeWindow(Long stockId, List<BigDecimal> closes) {
        if (closes == null || closes.isEmpty()) {
            log.warn("Cannot initialize window for stock {} with empty closes", stockId);
            return;
        }

        List<BigDecimal> window = new ArrayList<>();
        if (closes.size() > WINDOW_SIZE) {
            window.addAll(closes.subList(closes.size() - WINDOW_SIZE, closes.size()));
        } else {
            window.addAll(closes);
        }
        
        indicatorWindowCache.put(stockId, Collections.synchronizedList(window));
        log.info("Initialized sliding window for stock {} with {} prices", stockId, window.size());
    }

    /**
     * Updates the sliding window and recalculates indicators.
     */
    @Transactional
    public void updateIndicators(Stock stock, BigDecimal newPrice) {
        updateIndicators(stock, newPrice, marketHoursService.getCurrentISTTime().toLocalDateTime());
    }

    /**
     * Updates indicators for a candle timestamp only if it belongs to a valid
     * trading session (trading day and market hours in IST).
     */
    @Transactional
    public void updateIndicators(Stock stock, BigDecimal newPrice, LocalDateTime candleTime) {
        if (!isEligibleIndicatorCandle(candleTime)) {
            log.debug("Skipping indicator update for {} at {} (non-trading or out-of-session candle)",
                    stock.getSymbol(), candleTime);
            return;
        }

        Long stockId = stock.getStockId();
        List<BigDecimal> window = indicatorWindowCache.getIfPresent(stockId);
        if (window == null) {
            window = Collections.synchronizedList(new ArrayList<>());
            indicatorWindowCache.put(stockId, window);
        }

        synchronized (window) {
            window.add(newPrice);
            if (window.size() > WINDOW_SIZE) {
                window.remove(0);
            }

            if (window.size() < WINDOW_SIZE) {
                log.debug("Insufficient data for indicators for stock {}: {}/{}", stockId, window.size(), WINDOW_SIZE);
                return;
            }

            calculateAndSave(stock, window);
        }
    }

    private boolean isEligibleIndicatorCandle(LocalDateTime candleTime) {
        if (candleTime == null) {
            return false;
        }

        LocalDate date = candleTime.toLocalDate();
        if (!marketHoursService.isTradingDay(date)) {
            return false;
        }

        LocalTime time = candleTime.toLocalTime();
        return !time.isBefore(marketHoursService.getMarketOpenTime())
                && !time.isAfter(marketHoursService.getMarketCloseTime());
    }

    private void calculateAndSave(Stock stock, List<BigDecimal> window) {
        BigDecimal rsi = calculateRSI(window);
        MACDResult macd = calculateMACD(window);

        String rsiVerdict = getRSIVerdict(rsi);
        String macdVerdict = getMACDVerdict(macd);
        String finalVerdict = getFinalVerdict(rsiVerdict, macdVerdict);

        StockIndicator indicator = indicatorRepository.findByStock_StockId(stock.getStockId())
                .orElse(StockIndicator.builder().stock(stock).build());

        indicator.setRsiValue(rsi);
        indicator.setRsiVerdict(rsiVerdict);
        indicator.setMacdValue(macd.macdLine);
        indicator.setMacdSignal(macd.signalLine);
        indicator.setMacdVerdict(macdVerdict);
        indicator.setFinalVerdict(finalVerdict);
        indicator.setLastUpdated(LocalDateTime.now());

        indicatorRepository.save(indicator);
        log.debug("Updated indicators for {}: RSI={}, MACD={}, Verdict={}", 
                stock.getSymbol(), rsi, macd.macdLine, finalVerdict);
    }

    private BigDecimal calculateRSI(List<BigDecimal> prices) {
        if (prices.size() <= RSI_PERIOD) return BigDecimal.ZERO;

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // Initial Avg Gain/Loss
        for (int i = 1; i <= RSI_PERIOD; i++) {
            BigDecimal diff = prices.get(i).subtract(prices.get(i - 1));
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(diff);
            } else {
                avgLoss = avgLoss.add(diff.abs());
            }
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(RSI_PERIOD), 4, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(RSI_PERIOD), 4, RoundingMode.HALF_UP);

        // Smoothed RSI calculation for the rest of the window
        for (int i = RSI_PERIOD + 1; i < prices.size(); i++) {
            BigDecimal diff = prices.get(i).subtract(prices.get(i - 1));
            BigDecimal gain = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
            BigDecimal loss = diff.compareTo(BigDecimal.ZERO) < 0 ? diff.abs() : BigDecimal.ZERO;

            avgGain = avgGain.multiply(BigDecimal.valueOf(RSI_PERIOD - 1))
                    .add(gain)
                    .divide(BigDecimal.valueOf(RSI_PERIOD), 4, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(RSI_PERIOD - 1))
                    .add(loss)
                    .divide(BigDecimal.valueOf(RSI_PERIOD), 4, RoundingMode.HALF_UP);
        }

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.valueOf(100);

        BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP));
    }

    private MACDResult calculateMACD(List<BigDecimal> prices) {
        List<BigDecimal> ema12 = calculateEMA(prices, MACD_FAST);
        List<BigDecimal> ema26 = calculateEMA(prices, MACD_SLOW);

        List<BigDecimal> macdLine = new ArrayList<>();
        int startIdx = MACD_SLOW - 1;
        for (int i = 0; i < ema26.size(); i++) {
            // ema12 and ema26 are calculated over the same window, but ema26 starts later effectively
            // Wait, calculateEMA returns a list of same size as prices?
            // Let's make calculateEMA return aligned values.
            macdLine.add(ema12.get(i + (MACD_SLOW - MACD_FAST)).subtract(ema26.get(i)));
        }
        
        // Now calculate signal line (EMA 9 of MACD line)
        List<BigDecimal> signalLine = calculateEMA(macdLine, MACD_SIGNAL);
        
        return new MACDResult(
            macdLine.get(macdLine.size() - 1).setScale(2, RoundingMode.HALF_UP),
            signalLine.get(signalLine.size() - 1).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private List<BigDecimal> calculateEMA(List<BigDecimal> data, int period) {
        List<BigDecimal> emaList = new ArrayList<>();
        if (data.size() < period) return emaList;

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        
        // Start with SMA as first EMA
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(data.get(i));
        }
        BigDecimal currentEma = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        emaList.add(currentEma);

        for (int i = period; i < data.size(); i++) {
            currentEma = data.get(i).subtract(currentEma)
                    .multiply(multiplier)
                    .add(currentEma);
            emaList.add(currentEma);
        }

        return emaList;
    }

    private String getRSIVerdict(BigDecimal rsi) {
        if (rsi.compareTo(BigDecimal.valueOf(70)) > 0) return "SELL";
        if (rsi.compareTo(BigDecimal.valueOf(30)) < 0) return "BUY";
        return "NEUTRAL";
    }

    private String getMACDVerdict(MACDResult macd) {
        if (macd.macdLine.compareTo(macd.signalLine) > 0) return "BUY";
        if (macd.macdLine.compareTo(macd.signalLine) < 0) return "SELL";
        return "NEUTRAL";
    }

    private String getFinalVerdict(String rsi, String macd) {
        if (rsi.equals("BUY") && macd.equals("BUY")) return "STRONG BUY";
        if (rsi.equals("SELL") && macd.equals("SELL")) return "STRONG SELL";
        if (rsi.equals("BUY") || macd.equals("BUY")) {
            if (rsi.equals("SELL") || macd.equals("SELL")) return "NEUTRAL";
            return "BUY";
        }
        if (rsi.equals("SELL") || macd.equals("SELL")) return "SELL";
        return "NEUTRAL";
    }

    private record MACDResult(BigDecimal macdLine, BigDecimal signalLine) {}
}
