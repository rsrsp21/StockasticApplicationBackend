package com.stockasticappbackend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stockasticappbackend.service.order.OrderService;
import com.stockasticappbackend.service.stockprice.MarketHoursService;
import com.stockasticappbackend.service.stockprice.StockPriceService;
import com.stockasticappbackend.service.wallet.FundSettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for periodic stock price operations.
 * Manages automated tasks for fetching prices, clearing old data, and cleanup.
 * 
 * Market Hours (IST):
 * - Trading Days: Monday to Friday (excluding holidays)
 * - Market Open: 9:15 AM
 * - Market Close: 3:30 PM
 * - Total intervals: 75 (every 5 minutes)
 * 
 * Scheduled Tasks:
 * - 9:00 AM on trading days: Clear previous day's data, keep closing price
 * - 9:00 AM on trading days: Process T+1 fund settlements
 * - Every 5 minutes (9:15-15:30): Fetch and store live prices
 * - 2:00 AM on Sundays: Cleanup old price records
 * 
 * Note: Startup settlement is handled by WalletSettlementInitializer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockPriceScheduler {

    private final StockPriceService stockPriceService;
    private final MarketHoursService marketHoursService;
    private final OrderService orderService;
    private final FundSettlementService fundSettlementService;

    /**
     * Clears previous day's data at 9:00 AM IST on trading days.
     * Retains only the last price row for each stock.
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void clearPreviousDayData() {
        log.info("9:00 AM IST - Checking if data cleanup is needed");
        try {
            stockPriceService.clearPreviousDayPrices();
        } catch (Exception e) {
            log.error("Error during 9:00 AM cleanup", e);
        }
    }

    /**
     * Processes T+1 fund settlements at 9:00 AM IST on trading days.
     * Moves unsettled funds older than 24 hours from lockedBalance to availableBalance.
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void processSettlements() {
        log.info("9:00 AM IST - Processing T+1 fund settlements");
        try {
            int settledCount = fundSettlementService.processSettlements();
            log.info("Fund settlement completed. {} funds settled.", settledCount);
        } catch (Exception e) {
            log.error("Error during fund settlement processing", e);
        }
    }

    /**
     * Executes pending AMO orders at Market Open (9:15 AM).
     */
    @Scheduled(cron = "0 15 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void executeAmoOrders() {
        log.info("9:15 AM IST - Triggering AMO Execution");
        try {
            if (!marketHoursService.isTodayTradingDay()) {
                log.info("Skipping AMO execution trigger: non-trading day.");
                return;
            }
            orderService.processDailyMarketOpenOrders();
        } catch (Exception e) {
            log.error("Error during AMO execution trigger", e);
        }
    }

    /**
     * Fetches and stores stock prices every 5 minutes during active market session.
     * Runs from 9:16 AM to 3:31 PM IST on Monday through Friday.
     */
    @Scheduled(cron = "0 1/5 9-15 * * MON-FRI", zone = "Asia/Kolkata")
    public void fetchStockPrices() {
        log.info("Scheduled price fetch triggered at IST: {}", marketHoursService.getCurrentISTTime());
        try {
            stockPriceService.fetchAndStorePrices();

            // Run pending-order checks only during active market session.
            if (marketHoursService.isMarketOpen()) {
                orderService.processDailyMarketOpenOrders();
            }
        } catch (Exception e) {
            log.error("Error during scheduled price fetch/execution", e);
        }
    }

    /**
     * Runs one reconciliation pass after close to fill missing latest candles.
     * Runs at 3:40 PM IST on trading days.
     */
    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void reconcileLatestCandleAfterClose() {
        log.info("Post-close latest-candle reconciliation triggered at IST: {}", marketHoursService.getCurrentISTTime());
        try {
            stockPriceService.reconcileLatestCandles();
        } catch (Exception e) {
            log.error("Error during post-close latest-candle reconciliation", e);
        }
    }

}
