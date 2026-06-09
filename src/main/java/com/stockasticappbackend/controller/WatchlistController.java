package com.stockasticappbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.watchlist.AddStockToWatchlistRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistCreateRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistDetailResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistItemResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistUpdateRequest;
import com.stockasticappbackend.service.watchlist.WatchlistService;
import com.stockasticappbackend.service.watchlist.WatchlistItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing user watchlists and watchlist items.
 * 
 * Provides endpoints for:
 * - CRUD operations on watchlists (create, read, update, delete)
 * - Managing stocks within watchlists (add, remove, list)
 * - Checking stock presence across user's watchlists
 * 
 * Base URL: /api/watchlists
 */
@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchlistController {
        private final WatchlistService watchlistService;
        private final WatchlistItemService watchlistItemService;

        /**
         * Retrieves all watchlists for a user with pagination.
         *
         * @param userId  The user's ID.
         * @param page    Page number (0-indexed, default: 0).
         * @param size    Page size (default: 10).
         * @param sortBy  Field to sort by (default: "createdAt").
         * @param sortDir Sort direction - "asc" or "desc" (default: "desc").
         * @return Paginated list of user's watchlists.
         */
        @GetMapping
        public ResponseEntity<PageResponse<WatchlistResponse>> getUserWatchlists(
                        @RequestParam Long userId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "created_at") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir) {

                return ResponseEntity.ok(
                                watchlistService.getUserWatchlistsPaged(
                                                userId, page, size, sortBy, sortDir));
        }

        /**
         * Retrieves a specific watchlist by ID.
         *
         * @param watchlistId The watchlist ID.
         * @param userId      The user's ID (for ownership verification).
         * @return Watchlist details including item count.
         */
        @GetMapping("/{watchlistId}")
        public ResponseEntity<WatchlistDetailResponse> getWatchlistById(
                        @PathVariable Long watchlistId,
                        @RequestParam Long userId) {

                return ResponseEntity.ok(
                                watchlistService.getWatchlistById(watchlistId, userId));
        }

        /**
         * Creates a new watchlist for a user.
         *
         * @param userId  The user's ID.
         * @param request Watchlist creation request containing name and description.
         * @return Created watchlist details with HTTP 201 status.
         */
        @PostMapping
        public ResponseEntity<WatchlistResponse> createWatchlist(
                        @RequestParam Long userId,
                        @Valid @RequestBody WatchlistCreateRequest request) {

                WatchlistResponse response = watchlistService.createWatchlist(userId, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Updates an existing watchlist's name or description.
         *
         * @param watchlistId The watchlist ID to update.
         * @param userId      The user's ID (for ownership verification).
         * @param request     Update request containing new name/description.
         * @return Updated watchlist details.
         */
        @PutMapping("/{watchlistId}")
        public ResponseEntity<WatchlistResponse> updateWatchlist(
                        @PathVariable Long watchlistId,
                        @RequestParam Long userId,
                        @Valid @RequestBody WatchlistUpdateRequest request) {

                return ResponseEntity.ok(
                                watchlistService.updateWatchlist(watchlistId, userId, request));
        }

        /**
         * Deletes a watchlist and all its items.
         *
         * @param watchlistId The watchlist ID to delete.
         * @param userId      The user's ID (for ownership verification).
         * @return HTTP 204 No Content on success.
         */
        @DeleteMapping("/{watchlistId}")
        public ResponseEntity<Void> deleteWatchlist(
                        @PathVariable Long watchlistId,
                        @RequestParam Long userId) {

                watchlistService.deleteWatchlist(watchlistId, userId);
                return ResponseEntity.noContent().build();
        }

        /**
         * Retrieves all stocks in a watchlist with pagination and live prices.
         *
         * @param watchlistId The watchlist ID.
         * @param userId      The user's ID (for ownership verification).
         * @param page        Page number (0-indexed, default: 0).
         * @param size        Page size (default: 10).
         * @param sortBy      Field to sort by (default: "addedAt").
         * @param sortDir     Sort direction - "asc" or "desc" (default: "desc").
         * @return Paginated list of watchlist items with current stock prices.
         */
        @GetMapping("/{watchlistId}/items")
        public ResponseEntity<PageResponse<WatchlistItemResponse>> getWatchlistItems(
                        @PathVariable Long watchlistId,
                        @RequestParam Long userId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "addedAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir) {

                return ResponseEntity.ok(
                                watchlistItemService.getWatchlistItemsWithPricesPaged(
                                                watchlistId, userId, page, size, sortBy, sortDir));
        }

        /**
         * Adds a stock to a watchlist.
         *
         * @param watchlistId The watchlist ID.
         * @param userId      The user's ID (for ownership verification).
         * @param request     Request containing the stock ID to add.
         * @return Added watchlist item with HTTP 201 status.
         */
        @PostMapping("/{watchlistId}/items")
        public ResponseEntity<WatchlistItemResponse> addStockToWatchlist(
                        @PathVariable Long watchlistId,
                        @RequestParam Long userId,
                        @Valid @RequestBody AddStockToWatchlistRequest request) {

                WatchlistItemResponse response = watchlistItemService.addStockToWatchlist(watchlistId, userId, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Removes a stock from a watchlist.
         *
         * @param watchlistId The watchlist ID.
         * @param stockId     The stock ID to remove.
         * @param userId      The user's ID (for ownership verification).
         * @return HTTP 204 No Content on success.
         */
        @DeleteMapping("/{watchlistId}/items/{stockId}")
        public ResponseEntity<Void> removeStockFromWatchlist(
                        @PathVariable Long watchlistId,
                        @PathVariable Long stockId,
                        @RequestParam Long userId) {

                watchlistItemService.removeStockFromWatchlist(
                                watchlistId, userId, stockId);

                return ResponseEntity.noContent().build();
        }

        /**
         * Checks if a stock exists in any of the user's watchlists.
         *
         * @param stockId The stock ID to check.
         * @param userId  The user's ID.
         * @return true if stock is in at least one watchlist, false otherwise.
         */
        @GetMapping("/contains/{stockId}")
        public ResponseEntity<Boolean> isStockInUserWatchlists(
                        @PathVariable Long stockId,
                        @RequestParam Long userId) {

                return ResponseEntity.ok(
                                watchlistItemService.isStockInUserWatchlists(stockId, userId));
        }

        /**
         * Gets list of watchlist IDs that contain a specific stock.
         * Used for checkbox selection in "Add to Watchlist" modal.
         *
         * @param stockId The stock ID to check.
         * @param userId  The user's ID.
         * @return List of watchlist IDs containing the stock.
         */
        @GetMapping("/stock/{stockId}/watchlist-ids")
        public ResponseEntity<java.util.List<Long>> getWatchlistIdsContainingStock(
                        @PathVariable Long stockId,
                        @RequestParam Long userId) {

                return ResponseEntity.ok(
                                watchlistItemService.getWatchlistIdsContainingStock(stockId, userId));
        }
}
