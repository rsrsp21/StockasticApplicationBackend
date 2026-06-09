package com.stockasticappbackend.config;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.stockprice.IndicatorService;
import com.stockasticappbackend.service.stockprice.MarketHoursService;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;
import com.stockasticappbackend.util.StockMarketTypeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes stock price data on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockPriceInitializer implements CommandLineRunner {

	private final StockPriceRepository stockPriceRepository;
	private final StockRepository stockRepository;
	private final MarketHoursService marketHoursService;
	private final YahooFinanceService yahooFinanceService;
	private final com.stockasticappbackend.service.growwapi.GrowwApiService growwApiService;
	private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final IndicatorService indicatorService;

    private static final int MAX_INTERVALS_PER_DAY = 76;
    private static final long INTERVAL_GRACE_PERIOD_MS = 3000;
    private static final int INTERVAL_EDGE_CASE_WINDOW_SECONDS = 10;
    @Value("${stockprice.initializer.parallelism:6}")
    private int initializerParallelism;

	/**
	 * Executes the stock price initialization logic on application startup.
	 *
	 * @param args Command line arguments (not used).
	 */
	@Override
	public void run(String... args) throws Exception {
		log.info("Starting StockPrice Initializer...");

        if (!areStartupTablesAvailable()) {
            log.warn("Skipping StockPrice initializer because required tables are missing: stock, stock_price.");
            return;
        }
        
        // Clean up any existing duplicates in a SEPARATE transaction first
        // This ensures cleanup commits before we start inserting
        TransactionTemplate cleanupTransaction = new TransactionTemplate(transactionManager);
        cleanupTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            cleanupTransaction.executeWithoutResult(status -> {
                try {
                    int deleted = stockPriceRepository.deleteDuplicateRecords();
                    if (deleted > 0) {
                       log.info("Startup Cleanup: Removed {} duplicate stock price records.", deleted);
                    }
                } catch (Exception e) {
                    log.warn("Startup cleanup failed: {}", e.getMessage());
                    status.setRollbackOnly();
                }
            });
        } catch (Exception e) {
            log.warn("Cleanup transaction failed (may be expected on first run): {}", e.getMessage());
        }

		log.info("=== Stock Price Initializer Started ===");
		log.info("Current IST Time: {}", marketHoursService.getCurrentISTTime());
		log.info("Is Market Open: {}", marketHoursService.isMarketOpen());
		log.info("Is After Market Close: {}", marketHoursService.isAfterMarketClose());
		log.info("Is Today Trading Day: {}", marketHoursService.isTodayTradingDay());
		log.info("Last Trading Day: {}", marketHoursService.getLastTradingDay());

		List<com.stockasticappbackend.model.entity.Stock> activeStocks = stockRepository.findByIsActiveTrue().stream()
                .filter(StockMarketTypeUtil::isDomestic)
                .toList();

		if (activeStocks.isEmpty()) {
			log.info("No active stocks found. Skipping price initialization.");
			return;
		}

		log.info("Found {} active stocks. Checking price data...", activeStocks.size());

        int parallelism = Math.max(1, Math.min(initializerParallelism, activeStocks.size()));
        log.info("Running StockPrice initializer with parallelism={}", parallelism);

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        List<Future<?>> tasks = new ArrayList<>();
        for (com.stockasticappbackend.model.entity.Stock stock : activeStocks) {
            tasks.add(executor.submit(() -> processSingleStock(stock)));
        }

        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception e) {
                log.error("Stock initialization worker failed", e);
            }
        }
        executor.shutdown();

		log.info("=== Stock Price Initializer Completed ===");
	}

    private void processSingleStock(com.stockasticappbackend.model.entity.Stock stock) {
        // Use REQUIRES_NEW for each stock so one failure does not affect others.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    initializeStockPrices(stock);
                    indicatorService.initializeIndicators(stock);
                } catch (Exception e) {
                    log.error("Error initializing stock {}", stock.getSymbol(), e);
                    status.setRollbackOnly();
                }
            });
        } catch (Exception e) {
            log.error("Failed to initialize prices for stock: {}", stock.getSymbol(), e);
        }
    }

    private boolean areStartupTablesAvailable() {
        try {
            Boolean hasStock = jdbcTemplate.queryForObject(
                    "SELECT to_regclass('public.stock') IS NOT NULL",
                    Boolean.class);
            Boolean hasStockPrice = jdbcTemplate.queryForObject(
                    "SELECT to_regclass('public.stock_price') IS NOT NULL",
                    Boolean.class);
            return Boolean.TRUE.equals(hasStock) && Boolean.TRUE.equals(hasStockPrice);
        } catch (Exception ex) {
            log.warn("Unable to verify table availability during startup: {}", ex.getMessage());
            return false;
        }
    }

	/**
	 * Initializes price data for a single stock based on current time.
	 */
	public void initializeStockPrices(com.stockasticappbackend.model.entity.Stock stock) throws IOException, InterruptedException {
		var latestPriceOpt = stockPriceRepository.findLatestByStockId(stock.getStockId());
		LocalDate today = marketHoursService.getCurrentISTTime().toLocalDate();
		LocalDate lastTradingDay = marketHoursService.getLastTradingDay();
		LocalTime currentTime = marketHoursService.getCurrentISTTime().toLocalTime();

		LocalTime preMarketStart = LocalTime.of(9, 0);
		LocalTime marketOpen = LocalTime.of(9, 15);

		boolean isTodayTradingDay = marketHoursService.isTradingDay(today);
		boolean isPreMarketWindow = isTodayTradingDay && currentTime.isAfter(preMarketStart.minusSeconds(1))
				&& currentTime.isBefore(marketOpen);
		boolean isMarketOpen = marketHoursService.isMarketOpen();
		boolean isAfterHours = !isMarketOpen && !isPreMarketWindow;

		log.info("Stock {} - Today: {}, Time: {}, TradingDay: {}, PreMarket: {}, MarketOpen: {}, AfterHours: {}",
				stock.getSymbol(), today, currentTime, isTodayTradingDay, isPreMarketWindow, isMarketOpen,
				isAfterHours);

		if (isPreMarketWindow) {
			log.info("[PRE-MARKET] Stock {} - Keeping only anchor row (previous close)", stock.getSymbol());
			keepOnlyAnchorRow(stock);
			return;
		}

		if (isMarketOpen) {
			log.info("[MARKET OPEN] Stock {} - Handling live trading hours", stock.getSymbol());

			if (latestPriceOpt.isPresent()) {
				LocalDate lastPriceDate = latestPriceOpt.get().getPriceTime().toLocalDate();

				// Case 1: Data exists for today - just complete missing rows
				if (lastPriceDate.equals(today)) {
					if (checkAndHandleIntervalEdgeCase(stock, currentTime)) {
						return;
					}

					long dailyCount = stockPriceRepository.countByStockIdAndDate(stock.getStockId(), today);
					int expectedRows = calculateExpectedRows();
					boolean hasMissingIntervals = hasMissingIntervalsForToday(stock, today, currentTime);

					if (dailyCount >= expectedRows && !hasMissingIntervals) {
						log.info("Stock {} has sufficient contiguous data ({} rows). Skipping.", stock.getSymbol(), dailyCount);
						return;
					}
					log.info("Stock {} needs reconciliation (rows={}/{}, missingIntervals={}). Fetching missing data...",
							stock.getSymbol(), dailyCount, expectedRows, hasMissingIntervals);
				} 
				// Case 2: Data is from previous trading day or STALE
				else {
					if (lastPriceDate.equals(lastTradingDay)) {
						log.info("Stock {} - New trading day {}. Clearing previous day {} data.", stock.getSymbol(), today,
								lastPriceDate);
					} else {
						log.warn("Stock {} has STALE data from {} (last trading day: {}, today: {}). Fetching fresh data.",
								stock.getSymbol(), lastPriceDate, lastTradingDay, today);
					}
					stockPriceRepository.deleteByStock(stock);
				}
			} else {
				// Case 4: No data at all
				log.info("Stock {} has no data. Fetching today's data.", stock.getSymbol());
			}
			fetchAndSaveIntradayData(stock);
			return;
		}

		// SCENARIO 3: After Hours
		if (isAfterHours) {
			log.info("[AFTER HOURS] Stock {} - Ensuring complete previous day data", stock.getSymbol());

			if (latestPriceOpt.isPresent()) {
				LocalDate lastPriceDate = latestPriceOpt.get().getPriceTime().toLocalDate();

				if (lastPriceDate.equals(lastTradingDay)) {
					long dailyCount = stockPriceRepository.countByStockIdAndDate(stock.getStockId(), lastTradingDay);
					boolean hasMissingIntervals = hasMissingIntervalsForToday(stock, lastTradingDay, LocalTime.of(15, 30));

					if (dailyCount >= MAX_INTERVALS_PER_DAY && !hasMissingIntervals) {
						log.info("Stock {} has complete contiguous data ({} rows). Skipping.", stock.getSymbol(), dailyCount);
						return;
					}
					log.info("Stock {} needs previous-day reconciliation (rows={}/76, missingIntervals={}). Completing...",
							stock.getSymbol(), dailyCount, hasMissingIntervals);
				} else {
					log.info("Stock {} has stale data from {} (expected {}). Fetching fresh data.", stock.getSymbol(),
							lastPriceDate, lastTradingDay);
					stockPriceRepository.deleteByStock(stock);
				}
			} else {
				log.info("Stock {} has no data. Fetching last trading day's data.", stock.getSymbol());
			}
			fetchAndSaveIntradayData(stock);
			return;
		}

		// FALLBACK
		log.warn("[FALLBACK] Stock {} - Unexpected state. Fetching data.", stock.getSymbol());
		fetchAndSaveIntradayData(stock);
	}

	/**
	 * Checks if server started within first 10 seconds of a 5-minute interval.
	 */
	private boolean checkAndHandleIntervalEdgeCase(com.stockasticappbackend.model.entity.Stock stock, LocalTime currentTime) throws IOException {
		int minute = currentTime.getMinute();
		int second = currentTime.getSecond();

		// Check if we're at the NEW scheduled fetch time (minute % 5 == 1)
		// AND within first 10 seconds of that minute
		if (minute % 5 == 1 && second <= INTERVAL_EDGE_CASE_WINDOW_SECONDS) {
			log.warn(
					"[INTERVAL EDGE CASE] Stock {} - Server started at {}:{}:{} (interval boundary). "
							+ "Applying {}-second grace period to capture current interval data.",
					stock.getSymbol(), currentTime.getHour(), minute, second, INTERVAL_GRACE_PERIOD_MS / 1000);

			try {
				// Wait for Yahoo Finance to prepare the current interval's data
				Thread.sleep(INTERVAL_GRACE_PERIOD_MS);

				// Fetch again - should now include current interval (e.g., 3:10 PM)
				log.info("Grace period complete. Re-fetching to capture {}:{} interval data.", currentTime.getHour(),
						minute);
				fetchAndSaveIntradayData(stock);

				log.info("Interval edge case handled successfully for {}", stock.getSymbol());
				return true; // Edge case detected and handled
			} catch (InterruptedException e) {
				log.error("Interval edge case handling interrupted for {}", stock.getSymbol(), e);
				Thread.currentThread().interrupt();
				return false;
			}
		}

		return false; // No edge case detected
	}

	/**
	 * Keeps only the anchor row (last row from previous trading day).
	 */
	private void keepOnlyAnchorRow(com.stockasticappbackend.model.entity.Stock stock) {
		var latestPriceOpt = stockPriceRepository.findLatestByStockId(stock.getStockId());
		if (latestPriceOpt.isEmpty()) {
			log.warn("Stock {} has no data for anchor. Will show empty until market opens.", stock.getSymbol());
			return;
		}

		StockPrice anchor = latestPriceOpt.get();
		LocalDate today = marketHoursService.getCurrentISTTime().toLocalDate();

		if (anchor.getPriceTime().toLocalDate().equals(today)) {
			log.info("Stock {} has today's data in pre-market. Clearing all.", stock.getSymbol());
			stockPriceRepository.deleteByStock(stock);
			return;
		}

		log.info("Stock {} - Keeping anchor (ID: {}, Time: {})", stock.getSymbol(), anchor.getPriceId(),
				anchor.getPriceTime());

		List<StockPrice> allPrices = stockPriceRepository
				.findByStockOrderByPriceTimeDesc(stock, PageRequest.of(0, 1000)).getContent();

		int deletedCount = 0;
		List<StockPrice> toDelete = allPrices.stream().filter(price -> !price.getPriceId().equals(anchor.getPriceId()))
				.toList();

		if (!toDelete.isEmpty()) {
			stockPriceRepository.deleteAllInBatch(toDelete);
			deletedCount = toDelete.size();
		}

		anchor.setPreviousClose(anchor.getIntervalClose());
		stockPriceRepository.save(anchor);

		log.info("Stock {} - Deleted {} old rows, kept 1 anchor row with change = 0%.", stock.getSymbol(),
				deletedCount);
	}

	/**
	 * Fetches intraday data from Yahoo Finance and saves it to the database.
	 */
	private void fetchAndSaveIntradayData(com.stockasticappbackend.model.entity.Stock stock) throws IOException, InterruptedException {
		log.info("Fetching intraday data for {}...", stock.getSymbol());

		List<com.stockasticappbackend.service.yahoofinance.YahooQuote> intradayQuotes = yahooFinanceService.getIntradayQuotes(stock.getSymbol());

		if (intradayQuotes.isEmpty()) {
			log.info("Yahoo returned no intraday data for {}. Trying Groww fallback.", stock.getSymbol());
			List<com.stockasticappbackend.service.yahoofinance.YahooQuote> growwQuotes = growwApiService.getIntradayHistory(stock, 5);
			if (!growwQuotes.isEmpty()) {
				log.info("Groww fallback returned {} candles for {}", growwQuotes.size(), stock.getSymbol());
				intradayQuotes = growwQuotes;
			}
		}

		if (intradayQuotes.isEmpty()) {
			log.warn("No intraday data received for {}. Fetching single current quote.", stock.getSymbol());
			com.stockasticappbackend.service.yahoofinance.YahooQuote quote = yahooFinanceService.getQuote(stock.getSymbol());
			if (quote == null) {
				log.info("Yahoo quote failed for {}. Trying Groww latest snapshot.", stock.getSymbol());
				quote = growwApiService.getLatestSnapshot(stock);
			}
			if (quote != null) {
				saveQuoteIfNotExists(stock, quote, new HashSet<>());
				log.info("Saved 1 price record for {}", stock.getSymbol());
			}
			return;
		}

		// Track timestamps saved in THIS batch to prevent within-transaction duplicates
		Set<LocalDateTime> savedInBatch = new HashSet<>();

		int savedCount = 0;
		int skippedCount = 0;
		for (com.stockasticappbackend.service.yahoofinance.YahooQuote quote : intradayQuotes) {
			try {
				if (saveQuoteIfNotExists(stock, quote, savedInBatch)) {
					savedCount++;
				} else {
					skippedCount++;
				}
			} catch (Exception e) {
				log.trace("Failed to save quote for {}", stock.getSymbol());
			}
		}

		log.info("Stock {}: Saved {} new records, skipped {} existing records", stock.getSymbol(), savedCount,
				skippedCount);
	}

	/**
	 * Calculates the expected number of price rows based on current market time.
	 *
	 * @return The number of expected 5-minute intervals that should have been
	 *         recorded.
	 */
	private int calculateExpectedRows() {
		if (!marketHoursService.isMarketOpen()) {
			return MAX_INTERVALS_PER_DAY;
		}

		LocalTime now = marketHoursService.getCurrentISTTime().toLocalTime();
		LocalTime firstCandleTime = LocalTime.of(9, 15);

		if (now.isBefore(firstCandleTime)) {
			return 0;
		}

		long minutes = Duration.between(firstCandleTime, now).toMinutes();

		int expected = (int) (minutes / 5) + 1;

		return Math.min(expected, MAX_INTERVALS_PER_DAY);
	}

	/**
	 * Checks whether any expected 5-minute interval from 9:15 AM until the current
	 * closed interval is missing for today.
	 */
	private boolean hasMissingIntervalsForToday(com.stockasticappbackend.model.entity.Stock stock, LocalDate date, LocalTime currentTime) {
		LocalTime firstCandleTime = LocalTime.of(9, 15);
		if (currentTime.isBefore(firstCandleTime)) {
			return false;
		}

		// Clamp to market close and align to last closed 5-minute bucket.
		LocalTime clamped = currentTime.isAfter(LocalTime.of(15, 30)) ? LocalTime.of(15, 30) : currentTime;
		int alignedMinute = (clamped.getMinute() / 5) * 5;
		LocalTime lastExpectedCandle = clamped.withMinute(alignedMinute).withSecond(0).withNano(0);

		Set<LocalDateTime> existing = stockPriceRepository.findByStockIdAndDate(stock.getStockId(), date).stream()
				.map(p -> p.getPriceTime().withSecond(0).withNano(0))
				.collect(Collectors.toSet());

		for (LocalDateTime ts = date.atTime(firstCandleTime); !ts.isAfter(date.atTime(lastExpectedCandle)); ts = ts
				.plusMinutes(5)) {
			if (!existing.contains(ts)) {
				log.debug("Stock {} missing expected interval at {}", stock.getSymbol(), ts);
				return true;
			}
		}
		return false;
	}

	/**
	 * Saves a quote only if it doesn't already exist.
	 * Uses existence check BEFORE save to avoid DataIntegrityViolationException
	 * which would taint the Hibernate transaction.
	 */
	private boolean saveQuoteIfNotExists(com.stockasticappbackend.model.entity.Stock stock, com.stockasticappbackend.service.yahoofinance.YahooQuote quote, Set<LocalDateTime> savedInBatch) {
		LocalDateTime quoteTime = quote.getPriceTime();

		int minute = quoteTime.getMinute();
		if (minute % 5 != 0) {
			log.debug("Stock {} - Skipping non-interval timestamp: {} (minute={})", stock.getSymbol(),
					quoteTime, minute);
			return false;
		}

		LocalDateTime normalizedTime = quoteTime.withSecond(0).withNano(0);

		if (savedInBatch.contains(normalizedTime)) {
			log.debug("Stock {} - Skipping within-batch duplicate: {}", stock.getSymbol(), normalizedTime);
			return false;
		}

		LocalDate date = quoteTime.toLocalDate();
		int hour = quoteTime.getHour();

		boolean existsForMinute = stockPriceRepository.existsByStockIdAndMinute(stock.getStockId(), date, hour, minute);

		if (existsForMinute) {
			log.debug("Stock {} - Skipping duplicate minute record: {} (date={}, hour={}, minute={})",
					stock.getSymbol(), quoteTime, date, hour, minute);
			savedInBatch.add(normalizedTime); // Mark as processed to avoid re-checking
			return false;
		}

		// Double-check using exact timestamp before save
		boolean existsByExactTime = stockPriceRepository.existsByStockAndPriceTime(stock, normalizedTime);
		if (existsByExactTime) {
			log.debug("Stock {} - Skipping exact timestamp duplicate: {}", stock.getSymbol(), normalizedTime);
			savedInBatch.add(normalizedTime);
			return false;
		}

		StockPrice stockPrice = StockPrice.builder().stock(stock)
				.previousClose(quote.getPreviousClose()).dayHigh(quote.getDayHigh())
				.dayLow(quote.getDayLow())
                .fiftyTwoWeekHigh(quote.getFiftyTwoWeekHigh())
                .fiftyTwoWeekLow(quote.getFiftyTwoWeekLow())
                .intervalOpen(quote.getIntervalOpen())
                .intervalHigh(quote.getIntervalHigh())
                .intervalLow(quote.getIntervalLow())
                .intervalClose(quote.getIntervalClose())
                .intervalVolume(quote.getIntervalVolume())
                .priceTime(normalizedTime)
				.build();

		stockPriceRepository.save(stockPrice);
		indicatorService.updateIndicators(stock, stockPrice.getIntervalClose(), normalizedTime);
		savedInBatch.add(normalizedTime);

		log.debug("Stock {} - Saved new price record: {} (normalized to {})", stock.getSymbol(), quoteTime,
				normalizedTime);
		return true;
	}

}
