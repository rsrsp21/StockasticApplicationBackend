package com.stockasticappbackend.service.stockprice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.stockasticappbackend.dto.stockprice.StockPriceHistoryResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.dto.stockprice.IndicatorSeriesResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.StockPriceMapper;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockIndicator;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.repository.StockIndicatorRepository;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;
import com.stockasticappbackend.service.yahoofinance.YahooQuote;
import com.stockasticappbackend.util.StockMarketTypeUtil;
import static com.stockasticappbackend.util.Constants.CANDLE_INTERVAL_MINUTES;
import static com.stockasticappbackend.util.Constants.HUNDRED;
import static com.stockasticappbackend.util.Constants.LOG_FAILED_CLEAR_PRICES;
import static com.stockasticappbackend.util.Constants.LOG_FAILED_FETCH_PRICE;
import static com.stockasticappbackend.util.Constants.LOG_FAILED_REFRESH_STOCK;
import static com.stockasticappbackend.util.Constants.MAX_PRICE_RECORDS_PER_STOCK;
import static com.stockasticappbackend.util.Constants.NO_PRICE_DATA_ID;
import static com.stockasticappbackend.util.Constants.NO_PRICE_DATA_SYMBOL;
import static com.stockasticappbackend.util.Constants.STOCK_NOT_FOUND_ID;
import static com.stockasticappbackend.util.Constants.STOCK_NOT_FOUND_SYMBOL;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockPriceServiceImpl implements StockPriceService {

	private final StockPriceRepository stockPriceRepository;
	private final StockRepository stockRepository;
	private final StockPriceMapper stockPriceMapper;
	private final YahooFinanceService yahooFinanceService;
	private final MarketHoursService marketHoursService;
    private final IndicatorService indicatorService;
    private final StockIndicatorRepository stockIndicatorRepository;
    private final PlatformTransactionManager transactionManager;

	@Value("${stockprice.cleanup.days-to-keep:30}")
	private int defaultDaysToKeep;
    private static final int RETRY_ROUND_ONE_DELAY_MS = 800;
    private static final int RETRY_ROUND_TWO_DELAY_MS = 1800;
    private static final int MAX_RETRY_STOCKS_PER_ROUND = 200;
    private static final int RECENT_RECONCILE_BUCKETS = 6;
    private static final int INDICATOR_WARMUP_CANDLES = 50;

	/**
	 * Fetches the latest price for a stock by ID
	 */
	@Override
	@Cacheable(value = "latestPriceById", key = "#stockId")
	public StockPriceResponse getLatestPrice(Long stockId) {
		log.debug("Fetching latest price for stock ID: {}", stockId);

		Stock stock = stockRepository.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException(STOCK_NOT_FOUND_ID + stockId));

		ensureDataExists(stock);

		StockPrice stockPrice = stockPriceRepository.findLatestByStockId(stockId)
				.orElseThrow(() -> new ResourceNotFoundException(NO_PRICE_DATA_ID + stockId));
		
        StockPriceResponse response = mapWithChangePercent(stockPrice);
        Long totalVolume = stockPriceRepository.sumVolumeByStockIdAndDate(stockId, stockPrice.getPriceTime().toLocalDate());
        response.setVolume(totalVolume != null ? totalVolume : stockPrice.getIntervalVolume());
        
        return response;
	}

	/**
	 * Fetches the latest price for a stock by symbol
	 */
	@Override
	public StockPriceResponse getLatestPriceBySymbol(String symbol) {
		log.debug("Fetching latest price for symbol: {}", symbol);

		Stock stock = stockRepository.findBySymbol(symbol)
				.orElseThrow(() -> new ResourceNotFoundException(STOCK_NOT_FOUND_SYMBOL + symbol));

		ensureDataExists(stock);

		StockPrice stockPrice = stockPriceRepository.findLatestBySymbol(symbol)
				.orElseThrow(() -> new ResourceNotFoundException(NO_PRICE_DATA_SYMBOL + symbol));
		return mapWithChangePercent(stockPrice);
	}

	/**
	 * Fetches latest prices for all active stocks
	 */
	@Override
	@Cacheable("latestPrices")
	public List<StockPriceResponse> getAllLatestPrices() {
		log.debug("Fetching latest prices for all active stocks");

		List<StockPrice> latestPrices = stockPriceRepository.findLatestPricesForActiveStocks();
        Set<Long> stockIds = latestPrices.stream()
                .map(sp -> sp.getStock().getStockId())
                .collect(Collectors.toSet());

        Map<Long, StockIndicator> indicatorsByStockId = stockIds.isEmpty() ? Map.of()
                : stockIndicatorRepository.findByStock_StockIdIn(stockIds).stream()
                        .collect(Collectors.toMap(ind -> ind.getStock().getStockId(), ind -> ind));

		return latestPrices.stream()
                .map(stockPrice -> mapWithChangePercent(stockPrice,
                        indicatorsByStockId.get(stockPrice.getStock().getStockId())))
                .collect(Collectors.toList());
	}

	/**
	 * Fetches price history for a specific stock within a time range
	 */
	@Override
	public StockPriceHistoryResponse getPriceHistory(Long stockId, LocalDateTime startTime, LocalDateTime endTime) {
		log.debug("Fetching price history for stock ID: {} from {} to {}", stockId, startTime, endTime);

		Stock stock = stockRepository.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException(STOCK_NOT_FOUND_ID + stockId));

		ensureDataExists(stock);

		List<StockPrice> priceHistory = stockPriceRepository.findPriceHistoryByStockId(stockId, startTime, endTime);

		return StockPriceHistoryResponse.builder().stockId(stock.getStockId()).symbol(stock.getSymbol())
				.stockName(stock.getName()).priceHistory(stockPriceMapper.toPricePointList(priceHistory)).build();
	}

    /**
     * Builds indicator series for chart plotting.
     * Uses prior candles as warm-up and emits only requested display timestamps.
     */
    @Override
    public IndicatorSeriesResponse getIndicatorSeries(Long stockId, String range) {
        String normalizedRange = (range == null || range.isBlank()) ? "1D" : range.toUpperCase();
        if (!"1D".equals(normalizedRange)) {
            normalizedRange = "1D";
        }

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException(STOCK_NOT_FOUND_ID + stockId));

        LocalDate targetDate = resolveIndicatorTargetDate();
        LocalDateTime displayStart = targetDate.atTime(marketHoursService.getMarketOpenTime());
        LocalDateTime displayEnd = targetDate.atTime(marketHoursService.getMarketCloseTime());

        List<StockPrice> display = stockPriceRepository.findPriceHistoryByStockId(stockId, displayStart, displayEnd);
        if (display.isEmpty()) {
            return IndicatorSeriesResponse.builder()
                    .stockId(stockId)
                    .symbol(stock.getSymbol())
                    .range(normalizedRange)
                    .points(List.of())
                    .build();
        }

        List<StockPrice> warmup = stockPriceRepository.findRecentPricesBeforeTime(
                stockId, displayStart, INDICATOR_WARMUP_CANDLES);
        Collections.reverse(warmup); // query is DESC; indicators need ASC order

        List<StockPrice> combined = new ArrayList<>(warmup.size() + display.size());
        combined.addAll(warmup);
        combined.addAll(display);

        List<Double> closes = combined.stream()
                .map(sp -> sp.getIntervalClose().doubleValue())
                .collect(Collectors.toList());

        List<Double> rsiSeries = calculateRsiSeries(closes, 14);
        List<Double> ema12 = calculateEmaSeries(closes, 12);
        List<Double> ema26 = calculateEmaSeries(closes, 26);

        List<Double> macdSeries = new ArrayList<>(closes.size());
        for (int i = 0; i < closes.size(); i++) {
            macdSeries.add(ema12.get(i) - ema26.get(i));
        }
        List<Double> signalSeries = calculateEmaSeries(macdSeries, 9);

        int warmupSize = warmup.size();
        List<IndicatorSeriesResponse.IndicatorPoint> points = new ArrayList<>(display.size());
        for (int i = warmupSize; i < combined.size(); i++) {
            Double rsi = rsiSeries.get(i);
            Double macd = macdSeries.get(i);
            Double signal = signalSeries.get(i);
            Double hist = (macd == null || signal == null) ? null : (macd - signal);

            points.add(IndicatorSeriesResponse.IndicatorPoint.builder()
                    .time(combined.get(i).getPriceTime())
                    .rsi(scale2(rsi))
                    .macd(scale2(macd))
                    .signal(scale2(signal))
                    .histogram(scale2(hist))
                    .build());
        }

        return IndicatorSeriesResponse.builder()
                .stockId(stockId)
                .symbol(stock.getSymbol())
                .range(normalizedRange)
                .points(points)
                .build();
    }

    private LocalDate resolveIndicatorTargetDate() {
        LocalDate today = marketHoursService.getCurrentISTTime().toLocalDate();
        if (!marketHoursService.isTradingDay(today) || marketHoursService.isBeforeMarketOpen()) {
            return marketHoursService.getLastTradingDay();
        }
        return today;
    }

    private List<Double> calculateEmaSeries(List<Double> values, int period) {
        List<Double> ema = new ArrayList<>(values.size());
        if (values.isEmpty()) {
            return ema;
        }
        double multiplier = 2.0 / (period + 1.0);
        double prev = values.get(0);
        ema.add(prev);
        for (int i = 1; i < values.size(); i++) {
            double current = ((values.get(i) - prev) * multiplier) + prev;
            ema.add(current);
            prev = current;
        }
        return ema;
    }

    private List<Double> calculateRsiSeries(List<Double> closes, int period) {
        List<Double> rsi = new ArrayList<>(Collections.nCopies(closes.size(), null));
        if (closes.size() <= period) {
            return rsi;
        }

        double gains = 0.0;
        double losses = 0.0;
        for (int i = 1; i <= period; i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff > 0) gains += diff;
            else losses += Math.abs(diff);
        }

        double avgGain = gains / period;
        double avgLoss = losses / period;
        rsi.set(period, avgLoss == 0 ? 100.0 : (100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))));

        for (int i = period + 1; i < closes.size(); i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            double gain = Math.max(diff, 0.0);
            double loss = Math.max(-diff, 0.0);
            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;
            rsi.set(i, avgLoss == 0 ? 100.0 : (100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))));
        }

        return rsi;
    }

    private BigDecimal scale2(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

	/**
	 * Ensures stock price data exists and is up-to-date
	 * 
	 * This is a LIGHTWEIGHT check - only triggers refresh if: 1. No data exists at
	 * all for the stock, OR 2. Data is from a different day than the last trading
	 * day
	 * 
	 * Does NOT check for staleness within the same day (scheduler handles that).
	 * 
	 * @param stock The stock to check.
	 */
	private void ensureDataExists(Stock stock) {
		var latestPrice = stockPriceRepository.findLatestByStockId(stock.getStockId());

		if (latestPrice.isEmpty()) {
			log.warn("No data for {}. Fetching on-demand...", stock.getSymbol());
			refreshStockData(stock);
			return;
		}

		// Check if data is from the correct trading day
		LocalDate dataDate = latestPrice.get().getPriceTime().toLocalDate();
		LocalDate today = marketHoursService.getCurrentISTTime().toLocalDate();
		LocalDate lastTradingDay = marketHoursService.getLastTradingDay();

		// Data should be from today OR lastTradingDay (if market hasn't opened yet today)
		boolean isValidDate = dataDate.equals(today) || dataDate.equals(lastTradingDay);

		if (!isValidDate) {
			log.warn("Data for {} is from {} (expected {} or {}). Refreshing...", stock.getSymbol(), dataDate, today,
					lastTradingDay);
			refreshStockData(stock);
		}
	}

	/**
	 * Maps Entity to DTO within change percentage
	 */
	private StockPriceResponse mapWithChangePercent(StockPrice stockPrice) {
        return mapWithChangePercent(stockPrice,
                stockIndicatorRepository.findByStock_StockId(stockPrice.getStock().getStockId()).orElse(null));
    }

    private StockPriceResponse mapWithChangePercent(StockPrice stockPrice, StockIndicator indicator) {
		StockPriceResponse response = stockPriceMapper.toResponse(stockPrice);
		response.setChangePercent(calculateChangePercent(stockPrice));
        
        // Enrich with Technical Indicators
        if (indicator != null) {
            response.setRsiValue(indicator.getRsiValue());
            response.setRsiVerdict(indicator.getRsiVerdict());
            response.setMacdValue(indicator.getMacdValue());
            response.setMacdSignal(indicator.getMacdSignal());
            response.setMacdVerdict(indicator.getMacdVerdict());
            response.setFinalVerdict(indicator.getFinalVerdict());
        }
        
		return response;
	}

	/**
	 * Helper to calculate price change percentage
	 */
	private BigDecimal calculateChangePercent(StockPrice stockPrice) {
		BigDecimal baseline = stockPrice.getPreviousClose();
		if (baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
			baseline = stockPrice.getIntervalOpen();
		}

		if (baseline != null && baseline.compareTo(BigDecimal.ZERO) != 0 && stockPrice.getIntervalClose() != null) {
			BigDecimal change = stockPrice.getIntervalClose().subtract(baseline);
			return change.divide(baseline, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(HUNDRED));
		}
		return null;
	}

	/**
	 * Refreshes stock data by fetching fresh intraday quotes
	 * 
	 * Uses in-memory Set to prevent duplicates when Yahoo returns the
	 * same candle twice (e.g., 3:30 PM closing candle appearing twice).
	 */
	private void refreshStockData(Stock stock) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setReadOnly(false);
        transactionTemplate.executeWithoutResult(status -> refreshStockDataInWriteTransaction(stock));
	}

    private void refreshStockDataInWriteTransaction(Stock stock) {
		try {
			// Clear old data
			stockPriceRepository.deleteByStock(stock);

			// Fetch fresh intraday data
			List<YahooQuote> intradayQuotes = yahooFinanceService.getIntradayQuotes(stock.getSymbol());

			if (!intradayQuotes.isEmpty()) {
				// Track timestamps saved in this batch to prevent duplicates
				java.util.Set<LocalDateTime> savedInBatch = new java.util.HashSet<>();
				int savedCount = 0;
				int skippedCount = 0;

				for (YahooQuote quote : intradayQuotes) {
					LocalDateTime quoteTime = quote.getPriceTime();

					// Only save 5-minute interval candles with zero seconds
					if (quoteTime.getMinute() % CANDLE_INTERVAL_MINUTES != 0 || quoteTime.getSecond() != 0) {
						skippedCount++;
						continue;
					}

					// Normalize timestamp
					LocalDateTime normalizedTime = quoteTime.withSecond(0).withNano(0);

					// Skip if already saved in this batch
					if (savedInBatch.contains(normalizedTime)) {
						log.debug("Skipping duplicate {} for {}", normalizedTime, stock.getSymbol());
						skippedCount++;
						continue;
					}

					StockPrice newPrice = createStockPrice(stock, quote, normalizedTime);
                    
					if (!stockPriceRepository.existsByStockAndPriceTime(stock, normalizedTime)) {
                        try {
                            stockPriceRepository.save(newPrice);
                            indicatorService.updateIndicators(stock, newPrice.getIntervalClose(), normalizedTime);
                            savedCount++;
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            log.warn("Duplicate batch record avoided for {} at {}", stock.getSymbol(), normalizedTime);
                        }
                    } else {
                        skippedCount++;
                    }
					savedInBatch.add(normalizedTime);
				}
				log.info("Refreshed {} price records for {} (skipped {} duplicates)", savedCount, stock.getSymbol(),
						skippedCount);
			} else {
				// Fallback to single quote
				YahooQuote quote = yahooFinanceService.getQuote(stock.getSymbol());
				if (quote != null) {
					LocalDateTime normalizedTime = quote.getPriceTime().withSecond(0).withNano(0);
                    if (!stockPriceRepository.existsByStockAndPriceTime(stock, normalizedTime)) {
					    StockPrice stockPrice = createStockPrice(stock, quote, normalizedTime);
                        try {
					        stockPriceRepository.save(stockPrice);
					        log.info("Saved single quote for {}", stock.getSymbol());
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            log.warn("Duplicate single quote avoided for {} at {}", stock.getSymbol(), normalizedTime);
                        }
                    }
				}
			}
		} catch (Exception e) {
			log.error(LOG_FAILED_REFRESH_STOCK, stock.getSymbol(), e);
		}
	}

	/**
	 * Scheduled task to fetch and store prices
	 */
	@Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Caching(evict = {
			@CacheEvict(value = "latestPrices", allEntries = true),
			@CacheEvict(value = "latestPriceById", allEntries = true)
	})
	public void fetchAndStorePrices() {
		log.info("Starting scheduled price fetch from Yahoo Finance");

		// Check market hours, but allow a grace period for the final 3:30 PM candle fetch at 3:31 PM
        LocalDateTime istNow = marketHoursService.getCurrentISTTime().toLocalDateTime();
        boolean isMarketOpen = marketHoursService.isMarketOpen();
        boolean isTradingDay = marketHoursService.isTodayTradingDay();
        boolean isPostMarketFetch = istNow.getHour() == 15 && istNow.getMinute() == 31;

		if (!isTradingDay || (!isMarketOpen && !isPostMarketFetch)) {
			log.info("Skipping price fetch (tradingDay={}, marketOpen={}, postMarketFetch={}). Current IST time: {}",
                    isTradingDay,
                    isMarketOpen,
                    isPostMarketFetch,
					istNow);
			return;
		}

		List<Stock> activeStocks = stockRepository.findByIsActiveTrue().stream()
                .filter(StockMarketTypeUtil::isDomestic)
                .toList();
        LocalDate istToday = istNow.toLocalDate();
        LocalDateTime expectedLatestCandle = getExpectedLatestClosedCandle(istNow);

		int successCount = 0;
		int failCount = 0;
        Set<Long> unresolvedLatestAfterFirstPass = new LinkedHashSet<>();
        Map<Long, Stock> activeStocksById = activeStocks.stream()
                .collect(Collectors.toMap(Stock::getStockId, s -> s));

	        for (Stock stock : activeStocks) {
			try {

                // ANCHOR ROW CLEANUP:
                // If this is the START of a new trading day (no data for today yet),
                // we should clear the previous day's anchor row so the chart starts fresh.
                // Note: normalizedTime is getting deprecated in this new logic, we rely on Yahoo's timestamps
                boolean hasTodayData = stockPriceRepository.existsByStockIdAndDate(stock.getStockId(), istToday);

                if (!hasTodayData && isMarketOpen) {
                    var latestPriceOpt = stockPriceRepository.findLatestByStockId(stock.getStockId());
                    if (latestPriceOpt.isPresent()
                            && latestPriceOpt.get().getPriceTime().toLocalDate().isBefore(istToday)) {
                        log.info(
                                "No IST-today data for {} (latest: {}). Clearing previous day/anchor data before refill.",
                                stock.getSymbol(), latestPriceOpt.get().getPriceTime());
                        stockPriceRepository.deleteByStock(stock);
                    }
                }

                // Fetch Intraday History from Yahoo and save the latest candles
                // This ensures we get the official 5-min candle (O/H/L/C/V) not just a snapshot
                List<YahooQuote> intradayQuotes = yahooFinanceService.getIntradayQuotes(stock.getSymbol());

	                if (!intradayQuotes.isEmpty()) {
	                    // We only need to save the candles that are relevant from today
	                    // Yahoo range=1d returns today's candles.

                        Set<LocalDateTime> existingTimes = loadExistingCandleTimesForDate(stock.getStockId(), istToday);
	                    int stockSavedCount = saveCandlesFromQuotes(stock, intradayQuotes, existingTimes);
	                    int gapFilledCount = fillMissingCandlesForToday(stock, istToday, expectedLatestCandle, intradayQuotes, existingTimes);
	                    stockSavedCount += gapFilledCount;

                    if (stockSavedCount > 0) {
                        successCount++;
                        log.debug("Saved/Updated {} candles for {}", stockSavedCount, stock.getSymbol());
                    } else {
                        // If no new candles, it's still a success in terms of "fetch worked"
                        successCount++;
                    }

	                    if (expectedLatestCandle != null
	                            && (!existingTimes.contains(expectedLatestCandle)
	                                    || hasMissingRecentCandles(existingTimes, istToday, expectedLatestCandle,
	                                            RECENT_RECONCILE_BUCKETS))) {
	                        unresolvedLatestAfterFirstPass.add(stock.getStockId());
	                    }
                } else {
                    failCount++;
                    log.warn("No intraday data received for symbol: {}", stock.getSymbol());
                    if (expectedLatestCandle != null) {
                        unresolvedLatestAfterFirstPass.add(stock.getStockId());
                    }
                }

			} catch (Exception e) {
				failCount++;
				log.error(LOG_FAILED_FETCH_PRICE, stock.getSymbol(), e);
			}
		}

        int missingBeforeRetries = unresolvedLatestAfterFirstPass.size();
        Set<Long> unresolvedAfterRound1 = retryMissingLatestRound(unresolvedLatestAfterFirstPass, activeStocksById,
                istToday, expectedLatestCandle, RETRY_ROUND_ONE_DELAY_MS, "R1");
        Set<Long> unresolvedAfterRound2 = retryMissingLatestRound(unresolvedAfterRound1, activeStocksById,
                istToday, expectedLatestCandle, RETRY_ROUND_TWO_DELAY_MS, "R2");

        log.info("Latest-candle reconciliation for {}: missingBefore={}, afterR1={}, afterR2={}",
                expectedLatestCandle, missingBeforeRetries, unresolvedAfterRound1.size(), unresolvedAfterRound2.size());

		log.info("Price fetch completed. Success: {}, Failed: {}", successCount, failCount);
	}

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Caching(evict = {
            @CacheEvict(value = "latestPrices", allEntries = true),
            @CacheEvict(value = "latestPriceById", allEntries = true)
    })
    public void reconcileLatestCandles() {
        LocalDateTime istNow = marketHoursService.getCurrentISTTime().toLocalDateTime();
        boolean isTradingDay = marketHoursService.isTodayTradingDay();
        boolean isReconciliationTime = istNow.getHour() == 15 && istNow.getMinute() == 40;

        if (!isTradingDay || !isReconciliationTime) {
            log.info("Skipping latest-candle reconciliation (tradingDay={}, reconciliationTime={}). Current IST time: {}",
                    isTradingDay, isReconciliationTime, istNow);
            return;
        }

        List<Stock> activeStocks = stockRepository.findByIsActiveTrue().stream()
                .filter(StockMarketTypeUtil::isDomestic)
                .toList();
        LocalDate istToday = istNow.toLocalDate();
        LocalDateTime expectedLatestCandle = getExpectedLatestClosedCandle(istNow);
        if (expectedLatestCandle == null) {
            log.info("Skipping latest-candle reconciliation: no expected latest candle for current time {}", istNow);
            return;
        }

        Map<Long, Stock> activeStocksById = activeStocks.stream()
                .collect(Collectors.toMap(Stock::getStockId, s -> s));
        Set<Long> unresolvedLatestAfterFirstPass = new LinkedHashSet<>();

	        for (Stock stock : activeStocks) {
                Set<LocalDateTime> existingTimes = loadExistingCandleTimesForDate(stock.getStockId(), istToday);
	            if (!existingTimes.contains(expectedLatestCandle)
	                    || hasMissingRecentCandles(existingTimes, istToday, expectedLatestCandle, RECENT_RECONCILE_BUCKETS)) {
	                unresolvedLatestAfterFirstPass.add(stock.getStockId());
	            }
	        }

        int missingBeforeRetries = unresolvedLatestAfterFirstPass.size();
        Set<Long> unresolvedAfterRound1 = retryMissingLatestRound(unresolvedLatestAfterFirstPass, activeStocksById,
                istToday, expectedLatestCandle, RETRY_ROUND_ONE_DELAY_MS, "R1");
        Set<Long> unresolvedAfterRound2 = retryMissingLatestRound(unresolvedAfterRound1, activeStocksById,
                istToday, expectedLatestCandle, RETRY_ROUND_TWO_DELAY_MS, "R2");

        log.info("Latest-candle reconciliation for {}: missingBefore={}, afterR1={}, afterR2={}",
                expectedLatestCandle, missingBeforeRetries, unresolvedAfterRound1.size(), unresolvedAfterRound2.size());
    }

    private int saveCandlesFromQuotes(Stock stock, List<YahooQuote> quotes, Set<LocalDateTime> existingTimes) {
        int savedCount = 0;
        if (quotes == null || quotes.isEmpty()) {
            return 0;
        }

        for (YahooQuote quote : quotes) {
            LocalDateTime quoteTime = quote.getPriceTime();
            if (quoteTime.getMinute() % CANDLE_INTERVAL_MINUTES != 0) {
                continue;
            }

            LocalDateTime candleTime = quoteTime.withSecond(0).withNano(0);
            if (!existingTimes.contains(candleTime)) {
                try {
                    StockPrice stockPrice = createStockPrice(stock, quote, candleTime);
                    stockPriceRepository.save(stockPrice);
                    indicatorService.updateIndicators(stock, stockPrice.getIntervalClose(), candleTime);
                    existingTimes.add(candleTime);
                    savedCount++;
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Ignore duplicates if they slip through
                } catch (Exception e) {
                    log.debug("Skipping failed candle save for {} at {}: {}", stock.getSymbol(), candleTime,
                            e.getMessage());
                }
            }
        }
        return savedCount;
    }

    private Set<Long> retryMissingLatestRound(
            Set<Long> unresolvedStockIds,
            Map<Long, Stock> activeStocksById,
            LocalDate istToday,
            LocalDateTime expectedLatestCandle,
            int delayMs,
            String roundTag) {
        if (unresolvedStockIds == null || unresolvedStockIds.isEmpty() || expectedLatestCandle == null) {
            return Set.of();
        }

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry round {} interrupted before execution", roundTag);
            return unresolvedStockIds;
        }

        List<Long> stockIdsToProcess = unresolvedStockIds.stream()
                .limit(MAX_RETRY_STOCKS_PER_ROUND)
                .toList();

        Set<Long> stillUnresolved = new LinkedHashSet<>();
        int filledByRound = 0;
        int skippedByCap = Math.max(0, unresolvedStockIds.size() - stockIdsToProcess.size());

        for (Long stockId : stockIdsToProcess) {
            Stock stock = activeStocksById.get(stockId);
            if (stock == null) {
                continue;
            }

            try {
                List<YahooQuote> retryQuotes = yahooFinanceService.getIntradayQuotes(stock.getSymbol());
                Set<LocalDateTime> existingTimes = loadExistingCandleTimesForDate(stock.getStockId(), istToday);
                saveCandlesFromQuotes(stock, retryQuotes, existingTimes);
                fillMissingCandlesForToday(stock, istToday, expectedLatestCandle, retryQuotes, existingTimes);

                if (!existingTimes.contains(expectedLatestCandle)
                        || hasMissingRecentCandles(existingTimes, istToday, expectedLatestCandle, RECENT_RECONCILE_BUCKETS)) {
                    stillUnresolved.add(stockId);
                } else {
                    filledByRound++;
                }
            } catch (Exception e) {
                stillUnresolved.add(stockId);
                log.debug("Retry round {} failed for {}: {}", roundTag, stock.getSymbol(), e.getMessage());
            }
        }

        if (skippedByCap > 0) {
            unresolvedStockIds.stream()
                    .skip(stockIdsToProcess.size())
                    .forEach(stillUnresolved::add);
        }

        log.info("Retry {} done: input={}, processed={}, filled={}, unresolved={}, cappedOut={}",
                roundTag, unresolvedStockIds.size(), stockIdsToProcess.size(), filledByRound, stillUnresolved.size(),
                skippedByCap);

        return stillUnresolved;
    }

    private boolean hasMissingRecentCandles(Set<LocalDateTime> existing, LocalDate istToday, LocalDateTime expectedLatestCandle,
            int recentBuckets) {
        if (expectedLatestCandle == null || recentBuckets <= 0) {
            return false;
        }

        LocalDateTime firstCandle = istToday.atTime(9, 15);
        LocalDateTime start = expectedLatestCandle
                .minusMinutes((long) (recentBuckets - 1) * CANDLE_INTERVAL_MINUTES);
        if (start.isBefore(firstCandle)) {
            start = firstCandle;
        }

        for (LocalDateTime ts = start; !ts.isAfter(expectedLatestCandle); ts = ts
                .plusMinutes(CANDLE_INTERVAL_MINUTES)) {
            if (!existing.contains(ts)) {
                return true;
            }
        }
        return false;
    }

    private int fillMissingCandlesForToday(Stock stock, LocalDate istToday, LocalDateTime expectedLatestCandle,
            List<YahooQuote> candidateQuotes, Set<LocalDateTime> existingTimes) {
        if (expectedLatestCandle == null || !expectedLatestCandle.toLocalDate().equals(istToday)) {
            return 0;
        }

        LocalDateTime firstCandle = istToday.atTime(9, 15);
        if (expectedLatestCandle.isBefore(firstCandle)) {
            return 0;
        }

        List<LocalDateTime> missingBuckets = new ArrayList<>();
        for (LocalDateTime ts = firstCandle; !ts.isAfter(expectedLatestCandle); ts = ts
                .plusMinutes(CANDLE_INTERVAL_MINUTES)) {
            if (!existingTimes.contains(ts)) {
                missingBuckets.add(ts);
            }
        }

        if (missingBuckets.isEmpty()) {
            return 0;
        }

        Map<LocalDateTime, YahooQuote> quoteByBucket = buildQuoteBucketMap(candidateQuotes);

        boolean hasUncoveredBuckets = false;
        for (LocalDateTime bucket : missingBuckets) {
            if (!quoteByBucket.containsKey(bucket)) {
                hasUncoveredBuckets = true;
                break;
            }
        }
        if (hasUncoveredBuckets) {
            try {
                List<YahooQuote> refreshedQuotes = yahooFinanceService.getIntradayQuotes(stock.getSymbol());
                quoteByBucket = buildQuoteBucketMap(refreshedQuotes);
            } catch (Exception e) {
                log.debug("Gap-fill refresh failed for {}: {}", stock.getSymbol(), e.getMessage());
            }
        }

        int saved = 0;
        int unresolved = 0;
        for (LocalDateTime missingBucket : missingBuckets) {
            YahooQuote quote = quoteByBucket.get(missingBucket);
            if (quote == null) {
                unresolved++;
                continue;
            }

            if (!stockPriceRepository.existsByStockAndPriceTime(stock, missingBucket)) {
                StockPrice stockPrice = createStockPrice(stock, quote, missingBucket);
                try {
                    stockPriceRepository.save(stockPrice);
                    indicatorService.updateIndicators(stock, stockPrice.getIntervalClose(), missingBucket);
                    existingTimes.add(missingBucket);
                    saved++;
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Duplicate slipped through due to race; ignore.
                }
            }
        }

        if (saved > 0 || unresolved > 0) {
            log.info("Gap-fill for {}: missing={}, filled={}, unresolved={}",
                    stock.getSymbol(), missingBuckets.size(), saved, unresolved);
        }
        return saved;
    }

    private Set<LocalDateTime> loadExistingCandleTimesForDate(Long stockId, LocalDate date) {
        return stockPriceRepository.findByStockIdAndDate(stockId, date).stream()
                .map(p -> p.getPriceTime().withSecond(0).withNano(0))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<LocalDateTime, YahooQuote> buildQuoteBucketMap(List<YahooQuote> quotes) {
        Map<LocalDateTime, YahooQuote> bucketMap = new HashMap<>();
        if (quotes == null || quotes.isEmpty()) {
            return bucketMap;
        }

        for (YahooQuote quote : quotes) {
            LocalDateTime quoteTime = quote.getPriceTime();
            if (quoteTime.getMinute() % CANDLE_INTERVAL_MINUTES != 0) {
                continue;
            }

            LocalDateTime bucket = quoteTime.withSecond(0).withNano(0);
            YahooQuote existing = bucketMap.get(bucket);
            if (existing == null || quoteTime.isAfter(existing.getPriceTime())) {
                bucketMap.put(bucket, quote);
            }
        }
        return bucketMap;
    }

    private LocalDateTime getExpectedLatestClosedCandle(LocalDateTime istNow) {
        LocalTime nowTime = istNow.toLocalTime();
        LocalTime firstCandle = LocalTime.of(9, 15);
        LocalTime marketClose = marketHoursService.getMarketCloseTime();

        if (nowTime.isBefore(firstCandle.plusMinutes(1))) {
            return null;
        }

        LocalDateTime baseline = istNow.minusMinutes(1).withSecond(0).withNano(0);
        int flooredMinute = baseline.getMinute() - (baseline.getMinute() % CANDLE_INTERVAL_MINUTES);
        LocalDateTime expected = baseline.withMinute(flooredMinute);

        LocalDateTime first = istNow.toLocalDate().atTime(firstCandle);
        LocalDateTime close = istNow.toLocalDate().atTime(marketClose);
        if (expected.isBefore(first)) {
            return first;
        }
        if (expected.isAfter(close)) {
            return close;
        }
        return expected;
    }

	/**
	 * Forces a fresh fetch of all prices
	 */
	@Override
	@Caching(evict = {
			@CacheEvict(value = "latestPrices", allEntries = true),
			@CacheEvict(value = "latestPriceById", allEntries = true)
	})
	public void forceFetchPrices() {
		log.info("FORCE FETCH: Starting intraday price fetch from Yahoo Finance");

		List<Stock> activeStocks = stockRepository.findByIsActiveTrue().stream()
                .filter(StockMarketTypeUtil::isDomestic)
                .toList();
		int successCount = 0;
		int failCount = 0;

		for (Stock stock : activeStocks) {
			try {
				refreshStockData(stock);
				successCount++;
			} catch (Exception e) {
				failCount++;
				log.error(LOG_FAILED_FETCH_PRICE, stock.getSymbol(), e);
			}
		}

		log.info("FORCE FETCH completed. Success: {} stocks, Failed: {} stocks", successCount, failCount);
	}

	/**
	 * Clears old prices from previous days, keeping only closing price
	 */
	@Override
	public void clearPreviousDayPrices() {
		log.info("Checking if previous day's prices should be cleared...");

		if (!marketHoursService.isTodayTradingDay()) {
			log.info("Today is not a trading day (weekend/holiday). Keeping previous prices.");
			return;
		}

		log.info("Trading day - keeping only last row (closing price), deleting others");

		List<Stock> activeStocks = stockRepository.findByIsActiveTrue().stream()
                .filter(StockMarketTypeUtil::isDomestic)
                .toList();
		int clearedCount = 0;

		for (Stock stock : activeStocks) {
			try {
				var latestPrice = stockPriceRepository.findLatestByStockId(stock.getStockId());

				if (latestPrice.isPresent()) {
					StockPrice price = latestPrice.get();
					Long latestPriceId = price.getPriceId();

					List<StockPrice> allPrices = stockPriceRepository
							.findByStockOrderByPriceTimeDesc(stock, PageRequest.of(0, MAX_PRICE_RECORDS_PER_STOCK))
							.getContent();

					int deletedCount = 0;
					// Batch delete optimization
					List<StockPrice> toDelete = allPrices.stream()
							.filter(p -> !p.getPriceId().equals(latestPriceId)).toList();

					if (!toDelete.isEmpty()) {
						stockPriceRepository.deleteAllInBatch(toDelete);
						deletedCount = toDelete.size();
					}

					log.debug("Stock {}: Kept last row (ID: {}), deleted {} rows", stock.getSymbol(), latestPriceId,
							deletedCount);
					clearedCount++;
				} else {
					log.warn("No price data found for stock: {}", stock.getSymbol());
				}
			} catch (Exception e) {
				log.error(LOG_FAILED_CLEAR_PRICES, stock.getSymbol(), e);
			}
		}

		log.info("Reset complete for {} stocks. Each now has only the last price row.", clearedCount);
	}

	/**
	 * Deletes price records older than specified days
	 */
	@Override
	public void cleanupOldPrices(int daysToKeep) {
		int days = daysToKeep > 0 ? daysToKeep : defaultDaysToKeep;
		LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
		log.info("Cleaning up price records older than {} days (before {})", days, cutoffTime);
		stockPriceRepository.deleteByPriceTimeBefore(cutoffTime);
		log.info("Old price records cleaned up");
	}

	@Override
	public List<StockPriceResponse> getTopGainers(int limit) {
		return getAllLatestPrices().stream()
				.filter(sp -> sp.getChangePercent() != null)
				.sorted((s1, s2) -> s2.getChangePercent().compareTo(s1.getChangePercent())) // Descending
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	public List<StockPriceResponse> getTopLosers(int limit) {
		return getAllLatestPrices().stream()
				.filter(sp -> sp.getChangePercent() != null)
				.sorted((s1, s2) -> s1.getChangePercent().compareTo(s2.getChangePercent())) // Ascending
				.limit(limit)
				.collect(Collectors.toList());
	}

	/**
	 * Creates a StockPrice entity from YahooQuote
	 */
	private StockPrice createStockPrice(Stock stock, YahooQuote quote, LocalDateTime priceTime) {
		return StockPrice.builder()
				.stock(stock)
				.previousClose(quote.getPreviousClose())
				.dayHigh(quote.getDayHigh())
				.dayLow(quote.getDayLow())
                .fiftyTwoWeekHigh(quote.getFiftyTwoWeekHigh())
                .fiftyTwoWeekLow(quote.getFiftyTwoWeekLow())
                .intervalOpen(quote.getIntervalOpen())
                .intervalHigh(quote.getIntervalHigh())
                .intervalLow(quote.getIntervalLow())
                .intervalClose(quote.getIntervalClose())
                .intervalVolume(quote.getIntervalVolume())
				.priceTime(priceTime)
				.build();
	}

}
