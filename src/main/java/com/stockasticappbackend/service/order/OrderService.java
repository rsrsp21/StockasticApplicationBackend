package com.stockasticappbackend.service.order;

import java.util.List;

import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.dto.stock.StockResponse;

/**
 * Service interface for order operations.
 */
public interface OrderService {

    /**
     * Places a new order (buy or sell).
     *
     * @param email   The user's email.
     * @param request The order request.
     * @return The created order response.
     */
    OrderResponse placeOrder(String email, OrderRequest request);

    /**
     * Gets order history for a user.
     *
     * @param email The user's email.
     * @return List of order responses.
     */
    List<OrderResponse> getOrderHistory(String email);

    /**
     * Gets a specific order by ID.
     *
     * @param email   The user's email.
     * @param orderId The order ID.
     * @return The order response.
     */
    OrderResponse getOrder(String email, Long orderId);



    /**
     * Cancels a pending order and releases blocked funds/shares.
     *
     * @param email   The user's email.
     * @param orderId The order ID.
     * @return The cancelled order response.
     */
    OrderResponse cancelOrder(String email, Long orderId);

    /**
     * Processing pending orders at market open (AMO execution).
     * Should be triggered by scheduler at 9:15 AM.
     */
    void processDailyMarketOpenOrders();
    /**
     * Retrieves the most traded stocks based on order volume.
     *
     * @param limit The number of stocks to retrieve.
     * @return List of StockResponse for most traded stocks.
     */
    List<StockResponse> getMostTradedStocks(int limit);
}

