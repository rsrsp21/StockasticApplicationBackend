package com.stockasticappbackend.service.stockprice;

import java.time.LocalDateTime;
import java.util.List;

import com.stockasticappbackend.dto.stockprice.IndicatorSeriesResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceHistoryResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;

public interface StockPriceService {

    /**
     * Retrieves the latest stock price information for a given stock ID.
     *
     * @param stockId The unique identifier of the stock.
     * @return StockPriceResponse containing current price details.
     */
    StockPriceResponse getLatestPrice(Long stockId);

    /**
     * Retrieves the latest stock price information for a given stock symbol.
     *
     * @param symbol The ticker symbol of the stock (e.g., "AAPL").
     * @return StockPriceResponse containing current price details.
     */
    StockPriceResponse getLatestPriceBySymbol(String symbol);

    /**
     * Retrieves the latest price information for all currently active stocks.
     *
     * @return A list of StockPriceResponse objects for all active stocks.
     */
    List<StockPriceResponse> getAllLatestPrices();

    /**
     * Retrieves historical stock price data within a specified time range.
     *
     * @param stockId   The unique identifier of the stock.
     * @param startTime The start date and time of the history range.
     * @param endTime   The end date and time of the history range.
     * @return StockPriceHistoryResponse containing the list of historical prices.
     */
    StockPriceHistoryResponse getPriceHistory(Long stockId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Retrieves RSI/MACD indicator series for chart plotting.
     * Uses range data plus warm-up candles for stable initial values.
     *
     * @param stockId The stock ID.
     * @param range   The chart range (currently supports 1D).
     * @return Indicator series aligned to chart timestamps.
     */
    IndicatorSeriesResponse getIndicatorSeries(Long stockId, String range);

    /**
     * Triggers a fetch of the latest prices from the external provider (Yahoo
     * Finance)
     * and stores them in the database.
     * Typically called by a scheduled task.
     */
    void fetchAndStorePrices();

    /**
     * Runs post-close reconciliation for the latest market candle.
     * Intended to be triggered once after market close by a scheduler.
     */
    void reconcileLatestCandles();

    /**
     * Forces a fetch of the latest prices, bypassing market hour checks.
     * Intended for administrative or testing purposes.
     */
    void forceFetchPrices();

    /**
     * Clears stock price data from the previous trading day at market open.
     * This ensures the database reflects the fresh start of a new trading day.
     */
    void clearPreviousDayPrices();

    /**
     * Removes old stock price records to maintain database performance.
     *
     * @param daysToKeep The number of days of history to retain.
     */
    /**
     * Retrieves the top gaining stocks based on the latest 5-minute candles.
     *
     * @param limit The number of top gainers to retrieve.
     * @return List of StockPriceResponse for top gainers.
     */
    List<StockPriceResponse> getTopGainers(int limit);

    /**
     * Retrieves the top losing stocks based on the latest 5-minute candles.
     *
     * @param limit The number of top losers to retrieve.
     * @return List of StockPriceResponse for top losers.
     */
    List<StockPriceResponse> getTopLosers(int limit);

    void cleanupOldPrices(int daysToKeep);
}
