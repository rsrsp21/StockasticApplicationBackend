package com.stockasticappbackend.dto.watchlist;

import java.time.LocalDateTime;

import com.stockasticappbackend.dto.stock.StockResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for WatchlistItem - embeds full stock info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistItemResponse {
    private Long watchlistItemId;
    private Long watchlistId;
    private LocalDateTime addedAt;

    // Embed full stock details with prices
    private StockResponse stock;
}