package com.stockasticappbackend.controller;

import java.time.LocalDateTime;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.stockprice.IndicatorSeriesResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceHistoryResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.service.stockprice.InternationalMarketDataService;
import com.stockasticappbackend.service.stockprice.StockPriceService;
import com.stockasticappbackend.service.yahoofinance.ChartData;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for stock price data endpoints.
 * Provides access to real-time and historical stock price information.
 * Includes endpoints for both database-stored prices and live Yahoo Finance
 * data.
 */
@RestController
@RequestMapping("/stocks/prices")
@RequiredArgsConstructor
public class StockPriceController {

    private final StockPriceService stockPriceService;
    private final YahooFinanceService yahooFinanceService;
    private final InternationalMarketDataService internationalMarketDataService;

    /**
     * Retrieves the latest price for a specific stock by its unique ID.
     *
     * @param stockId The ID of the stock.
     * @return ResponseEntity containing the StockPriceResponse.
     */
    @GetMapping("/{stockId}/latest")
    public ResponseEntity<StockPriceResponse> getLatestPrice(@PathVariable Long stockId) {
        StockPriceResponse response = stockPriceService.getLatestPrice(stockId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the latest price for a specific stock by its symbol.
     *
     * @param symbol The stock symbol (e.g., "RELIANCE").
     * @return ResponseEntity containing the StockPriceResponse.
     */
    @GetMapping("/symbol/{symbol}/latest")
    public ResponseEntity<StockPriceResponse> getLatestPriceBySymbol(@PathVariable String symbol) {
        StockPriceResponse response = stockPriceService.getLatestPriceBySymbol(symbol);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the latest price information for all active stocks.
     *
     * @return ResponseEntity containing a list of StockPriceResponse objects.
     */
    @GetMapping("/latest")
    public ResponseEntity<List<StockPriceResponse>> getAllLatestPrices() {
        List<StockPriceResponse> response = stockPriceService.getAllLatestPrices();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves historical price data for a stock within a specified time range.
     *
     * @param stockId   The ID of the stock.
     * @param startTime The start of the historical range.
     * @param endTime   The end of the historical range.
     * @return ResponseEntity containing the StockPriceHistoryResponse.
     */
    @GetMapping("/{stockId}/history")
    public ResponseEntity<StockPriceHistoryResponse> getPriceHistory(
            @PathVariable Long stockId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        StockPriceHistoryResponse response = stockPriceService.getPriceHistory(stockId, startTime, endTime);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{stockId}/yahoo/latest")
    public ResponseEntity<StockPriceResponse> getYahooLatestPrice(@PathVariable Long stockId) {
        return ResponseEntity.ok(internationalMarketDataService.getLatestPrice(stockId));
    }

    @GetMapping("/{stockId}/yahoo/history")
    public ResponseEntity<StockPriceHistoryResponse> getYahooIntradayHistory(
            @PathVariable Long stockId,
            @RequestParam(defaultValue = "1d") String range) {
        return ResponseEntity.ok(internationalMarketDataService.getIntradayHistory(stockId, range));
    }

    @GetMapping("/{stockId}/yahoo/chart")
    public ResponseEntity<ChartData> getYahooChartData(
            @PathVariable Long stockId,
            @RequestParam(defaultValue = "1M") String range) {
        return ResponseEntity.ok(internationalMarketDataService.getHistoricalChart(stockId, range));
    }

    /**
     * Retrieves indicator series (RSI/MACD) for chart plotting.
     *
     * @param stockId The stock ID.
     * @param range   The chart range (currently 1D).
     * @return ResponseEntity containing indicator series.
     */
    @GetMapping("/{stockId}/indicators")
    public ResponseEntity<IndicatorSeriesResponse> getIndicatorSeries(
            @PathVariable Long stockId,
            @RequestParam(defaultValue = "1D") String range) {
        IndicatorSeriesResponse response = stockPriceService.getIndicatorSeries(stockId, range);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves 1-week historical chart data for a stock from Yahoo Finance.
     *
     * @param symbol The stock symbol.
     * @return ResponseEntity containing chart data.
     */
    @GetMapping("/chart/{symbol}/1w")
    public ResponseEntity<ChartData> getChart1Week(@PathVariable String symbol) {
        ChartData data = yahooFinanceService.getHistoricalData(symbol, "1W");
        return ResponseEntity.ok(data);
    }

    /**
     * Retrieves 1-month historical chart data for a stock from Yahoo Finance.
     *
     * @param symbol The stock symbol.
     * @return ResponseEntity containing chart data.
     */
    @GetMapping("/chart/{symbol}/1m")
    public ResponseEntity<ChartData> getChart1Month(@PathVariable String symbol) {
        ChartData data = yahooFinanceService.getHistoricalData(symbol, "1M");
        return ResponseEntity.ok(data);
    }

    /**
     * Retrieves 3-month historical chart data for a stock from Yahoo Finance.
     *
     * @param symbol The stock symbol.
     * @return ResponseEntity containing chart data.
     */
    @GetMapping("/chart/{symbol}/3m")
    public ResponseEntity<ChartData> getChart3Months(@PathVariable String symbol) {
        ChartData data = yahooFinanceService.getHistoricalData(symbol, "3M");
        return ResponseEntity.ok(data);
    }

    /**
     * Retrieves 1-year historical chart data for a stock from Yahoo Finance.
     *
     * @param symbol The stock symbol.
     * @return ResponseEntity containing chart data.
     */
    @GetMapping("/chart/{symbol}/1y")
    public ResponseEntity<ChartData> getChart1Year(@PathVariable String symbol) {
        ChartData data = yahooFinanceService.getHistoricalData(symbol, "1Y");
        return ResponseEntity.ok(data);
    }

    /**
     * Retrieves 3-year historical chart data for a stock from Yahoo Finance.
     *
     * @param symbol The stock symbol.
     * @return ResponseEntity containing chart data.
     */
    @GetMapping("/chart/{symbol}/3y")
    public ResponseEntity<ChartData> getChart3Years(@PathVariable String symbol) {
        ChartData data = yahooFinanceService.getHistoricalData(symbol, "3Y");
        return ResponseEntity.ok(data);
    }

    /**
     * Retrieves historical chart data for a stock with a custom range.
     * Supported ranges: 1W, 1M, 3M, 1Y, 3Y.
     *
     * @param symbol The stock symbol.
     * @param range  The time range for the data (default: "1M").
     * @return ResponseEntity containing chart data.
     */
    @GetMapping("/chart/{symbol}")
    public ResponseEntity<ChartData> getChartData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1M") String range) {
        ChartData data = yahooFinanceService.getHistoricalData(symbol, range);
        return ResponseEntity.ok(data);
    }
}
