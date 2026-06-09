package com.stockasticappbackend.service.watchlist;

import java.util.List;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistCreateRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistDetailResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistUpdateRequest;

/**
 * Service interface for watchlist operations.
 * Provides business logic for managing user watchlists.
 */
public interface WatchlistService {

        /**
         * Get all watchlists for a specific user.
         *
         * @param userId The ID of the user.
         * @return List of watchlist responses.
         */
        List<WatchlistResponse> getUserWatchlists(Long userId);

        /**
         * Get paginated watchlists for a specific user.
         *
         * @param userId  The ID of the user.
         * @param page    Page number.
         * @param size    Page size.
         * @param sortBy  Field to sort by.
         * @param sortDir Sort direction (asc/desc).
         * @return PageResponse of watchlist responses.
         */
        PageResponse<WatchlistResponse> getUserWatchlistsPaged(Long userId, int page, int size,
                        String sortBy, String sortDir);

        /**
         * Get a specific watchlist by ID.
         *
         * @param watchlistId The ID of the watchlist.
         * @param userId      The ID of the user (for authorization).
         * @return Detailed watchlist response.
         */
        WatchlistDetailResponse getWatchlistById(Long watchlistId, Long userId);

        /**
         * Create a new watchlist for a user.
         *
         * @param userId  The ID of the user.
         * @param request The watchlist creation request.
         * @return The created watchlist response.
         */
        WatchlistResponse createWatchlist(Long userId, WatchlistCreateRequest request);

        /**
         * Update an existing watchlist.
         *
         * @param watchlistId The ID of the watchlist.
         * @param userId      The ID of the user (for authorization).
         * @param request     The watchlist update request.
         * @return The updated watchlist response.
         */
        WatchlistResponse updateWatchlist(Long watchlistId, Long userId, WatchlistUpdateRequest request);

        /**
         * Delete a watchlist.
         *
         * @param watchlistId The ID of the watchlist.
         * @param userId      The ID of the user (for authorization).
         */
        void deleteWatchlist(Long watchlistId, Long userId);

}