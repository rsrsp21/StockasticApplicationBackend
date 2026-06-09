package com.stockasticappbackend.dto.stock;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating a stock.
 * Contains validation constraints for stock data fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockRequest {

    /** The stock ticker symbol (required, max 10 characters). */
    @NotBlank(message = Constants.STOCK_SYMBOL_REQUIRED)
    @Size(max = 10, message = Constants.SYMBOL_MAX_10)
    private String symbol;

    /** The full name of the company/stock (required). */
    @NotBlank(message = Constants.STOCK_NAME_REQUIRED)
    private String name;

    /** The exchange where the stock is traded (required, max 50 characters). */
    @NotBlank(message = Constants.EXCHANGE_REQUIRED)
    @Size(max = 50, message = Constants.EXCHANGE_MAX_50)
    private String exchange;

    /** The industry sector (optional, max 100 characters). */
    @Size(max = 100, message = Constants.SECTOR_MAX_100)
    private String sector;

    /** A brief description of the stock (optional, max 500 characters). */
    @Size(max = 500, message = Constants.DESCRIPTION_MAX_500)
    private String description;

    /** Whether the stock is active and visible to users. */
    private Boolean isActive = true;
}

