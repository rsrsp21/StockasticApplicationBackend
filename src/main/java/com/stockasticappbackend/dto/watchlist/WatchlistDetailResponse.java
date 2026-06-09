package com.stockasticappbackend.dto.watchlist;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Watchlist with detailed stock information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistDetailResponse {
    private Long watchlistId;
    private Long userId;
    private String name;
    private LocalDateTime createdAt;
    private List<WatchlistItemResponse> items;
}