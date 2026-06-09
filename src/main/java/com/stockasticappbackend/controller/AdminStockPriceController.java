package com.stockasticappbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.service.stockprice.StockPriceService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for administrative stock price management.
 * Provides endpoints for manual price fetching and database cleanup operations.
 * All endpoints require ADMIN role authentication.
 */
@RestController
@RequestMapping("/admin/stocks/prices")
@RequiredArgsConstructor
public class AdminStockPriceController {

    private final StockPriceService stockPriceService;

    /**
     * Manually triggers a price fetch from Yahoo Finance.
     * This respects market hours and will skip fetching if the market is closed.
     *
     * @return ResponseEntity with a confirmation message.
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> triggerPriceFetch() {
        stockPriceService.fetchAndStorePrices();
        return ResponseEntity.ok("Price fetch triggered (respects market hours)");
    }

    /**
     * Forces a price fetch from Yahoo Finance, bypassing market hour checks.
     * Use this for testing or populating initial data outside market hours.
     *
     * @return ResponseEntity with a confirmation message.
     */
    @PostMapping("/force-fetch")
    public ResponseEntity<String> forcePriceFetch() {
        stockPriceService.forceFetchPrices();
        return ResponseEntity.ok("FORCE price fetch completed (market hours bypassed)");
    }

    /**
     * Manually triggers cleanup of old price records.
     *
     * @param daysToKeep Number of days of price history to retain (default: 30).
     * @return ResponseEntity with a confirmation message.
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> triggerCleanup(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        stockPriceService.cleanupOldPrices(daysToKeep);
        return ResponseEntity.ok("Cleanup completed for records older than " + daysToKeep + " days");
    }

}
