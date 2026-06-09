package com.stockasticappbackend.dto.watchlist;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a stock to watchlist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStockToWatchlistRequest {
    @NotNull(message = Constants.STOCK_ID_REQUIRED)
    private Long stockId;
}

