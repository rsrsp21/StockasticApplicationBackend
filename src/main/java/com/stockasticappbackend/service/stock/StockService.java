package com.stockasticappbackend.service.stock;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.stock.BulkUploadResponse;
import com.stockasticappbackend.dto.stock.StockRequest;
import com.stockasticappbackend.dto.stock.StockResponse;

/**
 * Service interface for stock management operations.
 */
public interface StockService {

        /**
         * Creates a new stock with optional image.
         *
         * @param stockRequest The stock creation request data.
         * @param image        The stock logo image (optional).
         * @return The created StockResponse.
         */
        StockResponse createStock(StockRequest stockRequest, MultipartFile image);

        /**
         * Bulk uploads stocks from a CSV or Excel file.
         * Processes each row independently — successful rows are saved even if others fail.
         * Stocks are processed in batches with flush/clear to prevent memory issues.
         *
         * @param file      The CSV or Excel file containing stock data.
         * @param imagesZip Optional ZIP file containing stock images named by symbol (e.g., AAPL.webp).
         * @return BulkUploadResponse with success/failure counts and error details.
         */
        BulkUploadResponse bulkUploadStocks(MultipartFile file, MultipartFile imagesZip);

        /**
         * Retrieves a stock by its unique ID.
         *
         * @param stockId The stock's ID.
         * @return The StockResponse.
         */
        StockResponse getStockById(Long stockId);


        /**
         * Retrieves a stock by its symbol.
         *
         * @param symbol The stock symbol.
         * @return The StockResponse.
         */
        StockResponse getStockBySymbol(String symbol);

        /**
         * Retrieves all stocks in the system.
         *
         * @return A list of StockResponse objects.
         */
        List<StockResponse> getAllStocks();

        /**
         * Retrieves all stocks with pagination (for admin).
         *
         * @param page    The page number.
         * @param size    The page size.
         * @param sortBy  The field to sort by.
         * @param sortDir The sort direction.
         * @return A PageResponse of StockResponse objects.
         */
        PageResponse<StockResponse> getAllStocksPaged(int page, int size, String sortBy, String sortDir);

        /**
         * Retrieves all active stocks.
         *
         * @return A list of active StockResponse objects.
         */
        List<StockResponse> getActiveStocks();

        /**
         * Retrieves active stocks with pagination.
         *
         * @param page    The page number.
         * @param size    The page size.
         * @param sortBy  The field to sort by.
         * @param sortDir The sort direction.
         * @return A PageResponse of active StockResponse objects.
         */
        PageResponse<StockResponse> getActiveStocksPaged(int page, int size, String sortBy, String sortDir);

        /**
         * Searches active stocks with optional filters.
         *
         * @param query     The search term.
         * @param sector    The sector filter.
         * @param exchange  The exchange filter.
         * @param minPrice  Min price filter.
         * @param maxPrice  Max price filter.
         * @param minVolume Min volume filter.
         * @param maxVolume Max volume filter.
         * @param page      The page number.
         * @param size      The page size.
         * @param sortBy    The field to sort by.
         * @param sortDir   The sort direction.
         * @return A PageResponse of matching StockResponse objects.
         */
        PageResponse<StockResponse> searchStocksWithFilters(String query, String sector, String exchange, String marketType,
                        BigDecimal minPrice, BigDecimal maxPrice, Long minVolume, Long maxVolume,
                        int page, int size, String sortBy, String sortDir);

        /**
         * Searches all stocks with optional filters (for admin).
         *
         * @param query     The search term.
         * @param sector    The sector filter.
         * @param exchange  The exchange filter.
         * @param minPrice  Min price filter.
         * @param maxPrice  Max price filter.
         * @param minVolume Min volume filter.
         * @param maxVolume Max volume filter.
         * @param page      The page number.
         * @param size      The page size.
         * @param sortBy    The field to sort by.
         * @param sortDir   The sort direction.
         * @return A PageResponse of matching StockResponse objects.
         */
        PageResponse<StockResponse> searchAllStocksWithFilters(String query, String sector, String exchange, String marketType,
                        BigDecimal minPrice, BigDecimal maxPrice, Long minVolume, Long maxVolume,
                        int page, int size, String sortBy, String sortDir);

        /**
         * Updates an existing stock with optional new image.
         *
         * @param stockId      The stock's ID.
         * @param stockRequest The update request data.
         * @param image        The new image (optional).
         * @return The updated StockResponse.
         */
        StockResponse updateStock(Long stockId, StockRequest stockRequest, MultipartFile image);

        /**
         * Toggles the active status of a stock.
         *
         * @param stockId The stock's ID.
         * @return The updated StockResponse.
         */
        StockResponse toggleStockStatus(Long stockId);

        /**
         * Deletes a stock from the system.
         *
         * @param stockId The stock's ID.
         */
        void deleteStock(Long stockId);
}
