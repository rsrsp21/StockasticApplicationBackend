package com.stockasticappbackend.dto.watchlist;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Watchlist with summary information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistResponse {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Integer stockCount;
}