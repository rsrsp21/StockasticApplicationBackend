package com.stockasticappbackend.service.holdings;

import java.math.BigDecimal;
import java.util.List;

import com.stockasticappbackend.dto.order.HoldingsResponse;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Stock;

/**
 * Service interface for managing user holdings/portfolio.
 */
public interface HoldingsService {

    /**
     * Retrieves the holdings portfolio for a user by email.
     *
     * @param email The user's email address.
     * @return A list of HoldingsResponse objects representing the user's portfolio.
     */
    List<HoldingsResponse> getHoldings(String email);

    /**
     * Retrieves a single holding for a user and specific stock.
     * Returns null if user doesn't hold this stock.
     *
     * @param email   The user's email address.
     * @param stockId The stock ID to look up.
     * @return HoldingsResponse for the stock, or null if not held.
     */
    HoldingsResponse getHoldingByStock(String email, Long stockId);

    /**
     * Validates if the user has sufficient available holdings to sell.
     * Throws InsufficientHoldingsException if validation fails.
     *
     * @param user     The user.
     * @param stock    The stock to sell.
     * @param quantity The quantity to sell.
     */
    void validateHoldingsForSell(AppUser user, Stock stock, int quantity);

    /**
     * Blocks shares in the user's holdings (moves from available to locked).
     *
     * @param user     The user.
     * @param stock    The stock.
     * @param quantity The quantity to block.
     */
    void blockShares(AppUser user, Stock stock, int quantity);

    /**
     * Releases blocked shares back to available quantity (e.g., on order cancellation).
     *
     * @param user     The user.
     * @param stock    The stock.
     * @param quantity The quantity to release.
     */
    void releaseBlockedShares(AppUser user, Stock stock, int quantity);

    /**
     * Credits holdings to the user's account after a successful buy order.
     * Updates average price and quantity.
     *
     * @param user           The user.
     * @param stock          The stock bought.
     * @param quantity       The quantity bought.
     * @param executedAmount The total executed amount (cost) for the buy order.
     */
    void creditHoldings(AppUser user, Stock stock, int quantity, BigDecimal executedAmount);

    /**
     * Debits holdings from the user's account after a successful sell order.
     * Updates realized P&L and removes holdings if quantity becomes zero.
     *
     * @param user            The user.
     * @param stock           The stock sold.
     * @param quantity        The quantity sold.
     * @param executedAmount  The total executed amount (revenue) from the sell order.
     * @param averagePrice    The average buy price (cost basis) for P&L calculation.
     * @return The realized P&L for this transaction.
     */
    BigDecimal debitHoldings(AppUser user, Stock stock, int quantity, BigDecimal executedAmount, BigDecimal averagePrice);
    
    /**
     * Gets the average buy price for a specific stock holding.
     * Used for calculating cost basis during sell execution.
     * 
     * @param user  The user.
     * @param stock The stock.
     * @return The average price.
     */
    BigDecimal getAveragePrice(AppUser user, Stock stock);
}
