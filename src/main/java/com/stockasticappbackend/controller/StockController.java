package com.stockasticappbackend.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.service.order.OrderService;
import com.stockasticappbackend.service.stock.StockService;
import com.stockasticappbackend.service.stockprice.StockPriceService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public stock information endpoints.
 * Provides read-only access to active stock data for authenticated users.
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockPriceService stockPriceService;
    private final OrderService orderService;

    /**
     * Retrieves all active stocks.
     *
     * @return ResponseEntity containing a list of StockResponse objects.
     */
    @GetMapping
    public ResponseEntity<List<StockResponse>> getActiveStocks() {
        List<StockResponse> stocks = stockService.getActiveStocks();
        return ResponseEntity.ok(stocks);
    }

    /**
     * Retrieves a paginated list of all active stocks.
     *
     * @param page    Page number (default: 0).
     * @param size    Number of records per page (default: 10).
     * @param sortBy  Field to sort by (default: "symbol").
     * @param sortDir Sort direction (default: "asc").
     * @return ResponseEntity containing a PageResponse of StockResponse objects.
     */
    @GetMapping("/paged")
    public ResponseEntity<PageResponse<StockResponse>> getActiveStocksPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "symbol") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<StockResponse> response = stockService.getActiveStocksPaged(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves details of a specific stock by its unique ID.
     *
     * @param stockId The ID of the stock.
     * @return ResponseEntity containing the StockResponse.
     */
    @GetMapping("/{stockId}")
    public ResponseEntity<StockResponse> getStockById(@PathVariable Long stockId) {
        StockResponse response = stockService.getStockById(stockId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves details of a specific stock by its symbol.
     *
     * @param symbol The stock symbol.
     * @return ResponseEntity containing the StockResponse.
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<StockResponse> getStockBySymbol(@PathVariable String symbol) {
        StockResponse response = stockService.getStockBySymbol(symbol);
        return ResponseEntity.ok(response);
    }

    /**
     * Searches for active stocks using various criteria with pagination.
     *
     * @param query    Search query matching name or symbol (optional).
     * @param sector   Sector filter (optional).
     * @param exchange Exchange filter (optional).
     * @param page     Page number (default: 0).
     * @param size     Page size (default: 10).
     * @param sortBy   Sort field (default: "symbol").
     * @param sortDir  Sort direction (default: "asc").
     * @return ResponseEntity containing a PageResponse of matching StockResponse
     *         objects.
     */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<StockResponse>> searchStocks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String marketType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long minVolume,
            @RequestParam(required = false) Long maxVolume,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "symbol") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<StockResponse> response = stockService.searchStocksWithFilters(
                query, sector, exchange, marketType, minPrice, maxPrice, minVolume, maxVolume, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the top gaining stocks.
     */
    @GetMapping("/market/gainers")
    public ResponseEntity<List<StockPriceResponse>> getTopGainers(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(stockPriceService.getTopGainers(limit));
    }

    /**
     * Retrieves the top losing stocks.
     */
    @GetMapping("/market/losers")
    public ResponseEntity<List<StockPriceResponse>> getTopLosers(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(stockPriceService.getTopLosers(limit));
    }

    /**
     * Retrieves the most traded stocks.
     */
    @GetMapping("/market/most-traded")
    public ResponseEntity<List<StockResponse>> getMostTradedStocks(
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(orderService.getMostTradedStocks(limit));
    }
}
