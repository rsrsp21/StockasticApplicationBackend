package com.stockasticappbackend.service.watchlist;

import java.util.List;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.watchlist.AddStockToWatchlistRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistItemResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;

/**
 * Service interface for watchlist item operations.
 * Provides business logic for managing stocks within watchlists.
 */
public interface WatchlistItemService {

    /**
     * Add a stock to a watchlist.
     *
     * @param watchlistId The ID of the watchlist.
     * @param userId      The ID of the user (for authorization).
     * @param request     The add stock request.
     * @return The created watchlist item response.
     */
    WatchlistItemResponse addStockToWatchlist(Long watchlistId, Long userId, AddStockToWatchlistRequest request);

    /**
     * Remove a stock from a watchlist.
     *
     * @param watchlistId The ID of the watchlist.
     * @param userId      The ID of the user (for authorization).
     * @param stockId     The ID of the stock to remove.
     */
    void removeStockFromWatchlist(Long watchlistId, Long userId, Long stockId);

    /**
     * Get all items in a watchlist with their current prices.
     *
     * @param watchlistId The ID of the watchlist.
     * @param userId      The ID of the user (for authorization).
     * @return List of watchlist items with current price data.
     */
    List<WatchlistItemResponse> getWatchlistItemsWithPrices(Long watchlistId, Long userId);

    /**
     * Get paginated items in a watchlist with their current prices.
     *
     * @param watchlistId The ID of the watchlist.
     * @param userId      The ID of the user (for authorization).
     * @param page        Page number.
     * @param size        Page size.
     * @param sortBy      Field to sort by.
     * @param sortDir     Sort direction (asc/desc).
     * @return PageResponse of watchlist items with current price data.
     */
    PageResponse<WatchlistItemResponse> getWatchlistItemsWithPricesPaged(Long watchlistId, Long userId,
            int page, int size,
            String sortBy, String sortDir);

    /**
     * Check if a stock exists in any of the user's watchlists.
     *
     * @param stockId The ID of the stock.
     * @param userId  The ID of the user.
     * @return true if the stock is in at least one watchlist, false otherwise.
     */
    boolean isStockInUserWatchlists(Long stockId, Long userId);

    /**
     * Get all watchlists that contain a specific stock for a user.
     *
     * @param stockId The ID of the stock.
     * @param userId  The ID of the user.
     * @return List of watchlists containing the stock.
     */
    List<WatchlistResponse> getWatchlistsContainingStock(Long stockId, Long userId);

    /**
     * Get list of watchlist IDs that contain a specific stock for a user.
     * Used for checkbox selection in UI.
     *
     * @param stockId The ID of the stock.
     * @param userId  The ID of the user.
     * @return List of watchlist IDs containing the stock.
     */
    List<Long> getWatchlistIdsContainingStock(Long stockId, Long userId);
}
