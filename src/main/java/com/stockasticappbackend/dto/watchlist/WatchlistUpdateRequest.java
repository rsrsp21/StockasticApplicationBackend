package com.stockasticappbackend.dto.watchlist;

import com.stockasticappbackend.util.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a watchlist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistUpdateRequest {
    @NotBlank(message = Constants.WATCHLIST_NAME_REQUIRED)
    @Size(max = 100, message = Constants.NAME_MAX_100_OR_LESS)
    private String name;
}

