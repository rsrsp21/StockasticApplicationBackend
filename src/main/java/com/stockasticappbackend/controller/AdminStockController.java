package com.stockasticappbackend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.stock.BulkUploadResponse;
import com.stockasticappbackend.dto.stock.StockRequest;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.service.stock.StockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for administrative stock management.
 * Provides full CRUD operations for stocks including creation, updates,
 * deletion, and status toggling. Supports image uploads for stock logos.
 * All endpoints require ADMIN role authentication.
 */
@RestController
@RequestMapping("/admin/stocks")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;

    /**
     * Creates a new stock entry in the system.
     *
     * @param stockRequest The stock creation request containing details.
     * @param image        The stock logo image file (optional).
     * @return ResponseEntity containing the created StockResponse with HTTP 201.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StockResponse> createStock(
            @ModelAttribute @Valid StockRequest stockRequest,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        StockResponse response = stockService.createStock(stockRequest, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Bulk uploads stocks from a CSV or Excel file with optional images ZIP.
     * Expected columns: symbol (required), name (required), exchange (required),
     * sector (optional), description (optional), isActive (optional).
     * Images ZIP should contain WEBP files named by symbol (e.g., AAPL.webp).
     *
     * @param file      The CSV or Excel (.xlsx, .xls) file containing stock data.
     * @param imagesZip Optional ZIP file containing stock images named by symbol.
     * @return ResponseEntity containing the BulkUploadResponse with HTTP 200.
     */
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponse> bulkUploadStocks(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imagesZip", required = false) MultipartFile imagesZip) {

        BulkUploadResponse response = stockService.bulkUploadStocks(file, imagesZip);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a list of all stocks in the system.
     *
     * @return ResponseEntity containing a list of StockResponse objects.
     */
    @GetMapping
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        List<StockResponse> stocks = stockService.getAllStocks();
        return ResponseEntity.ok(stocks);
    }

    /**
     * Retrieves a paginated list of all stocks.
     *
     * @param page    Page number (0-indexed, default: 0).
     * @param size    Number of records per page (default: 10).
     * @param sortBy  Field to sort by (default: "symbol").
     * @param sortDir Sort direction - "asc" or "desc" (default: "asc").
     * @return ResponseEntity containing a PageResponse of StockResponse objects.
     */
    @GetMapping("/paged")
    public ResponseEntity<PageResponse<StockResponse>> getAllStocksPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "symbol") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<StockResponse> response = stockService.getAllStocksPaged(page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves details of a specific stock by its unique ID.
     *
     * @param stockId The ID of the stock to retrieve.
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
     * @param symbol The exact stock symbol to search for.
     * @return ResponseEntity containing the StockResponse.
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<StockResponse> getStockBySymbol(@PathVariable String symbol) {
        StockResponse response = stockService.getStockBySymbol(symbol);
        return ResponseEntity.ok(response);
    }

    /**
     * Searches for stocks using various filters with pagination.
     *
     * @param query    Search term for symbol or name (optional).
     * @param sector   Filter by sector (optional).
     * @param exchange Filter by exchange (optional).
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
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Long minVolume,
            @RequestParam(required = false) Long maxVolume,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "symbol") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<StockResponse> response = stockService.searchAllStocksWithFilters(
                query, sector, exchange, null, minPrice, maxPrice, minVolume, maxVolume, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing stock's details.
     *
     * @param stockId      The ID of the stock to update.
     * @param stockRequest The update request containing new details.
     * @param image        The new image file (optional).
     * @return ResponseEntity containing the updated StockResponse.
     */
    @PutMapping(value = "/{stockId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StockResponse> updateStock(
            @PathVariable Long stockId,
            @ModelAttribute @Valid StockRequest stockRequest,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        StockResponse response = stockService.updateStock(stockId, stockRequest, image);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles the active status of a stock.
     *
     * @param stockId The ID of the stock to toggle.
     * @return ResponseEntity containing the updated StockResponse.
     */
    @PatchMapping("/{stockId}/toggle-status")
    public ResponseEntity<StockResponse> toggleStockStatus(@PathVariable Long stockId) {
        StockResponse response = stockService.toggleStockStatus(stockId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a stock from the system.
     *
     * @param stockId The ID of the stock to delete.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @DeleteMapping("/{stockId}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long stockId) {
        stockService.deleteStock(stockId);
        return ResponseEntity.noContent().build();
    }
}
