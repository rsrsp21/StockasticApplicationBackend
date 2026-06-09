package com.stockasticappbackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.order.HoldingsResponse;
import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.service.holdings.HoldingsService;
import com.stockasticappbackend.service.order.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for order operations.
 * Provides endpoints for placing orders and viewing holdings.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final HoldingsService holdingsService;

    /**
     * Places a new order (buy or sell).
     *
     * @param authentication The authenticated user.
     * @param request        The order request.
     * @return The created order response.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request) {
        String email = authentication.getName();
        OrderResponse response = orderService.placeOrder(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets order history for the authenticated user.
     *
     * @param authentication The authenticated user.
     * @return List of order responses.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrderHistory(Authentication authentication) {
        String email = authentication.getName();
        List<OrderResponse> orders = orderService.getOrderHistory(email);
        return ResponseEntity.ok(orders);
    }

    /**
     * Gets a specific order by ID.
     *
     * @param authentication The authenticated user.
     * @param orderId        The order ID.
     * @return The order response.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        String email = authentication.getName();
        OrderResponse response = orderService.getOrder(email, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets user's stock holdings/portfolio.
     *
     * @param authentication The authenticated user.
     * @return List of holdings responses.
     */
    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingsResponse>> getHoldings(Authentication authentication) {
        String email = authentication.getName();
        List<HoldingsResponse> holdings = holdingsService.getHoldings(email);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Gets a single holding by stock ID.
     * Returns 204 No Content if user doesn't hold this stock.
     *
     * @param authentication The authenticated user.
     * @param stockId        The stock ID to look up.
     * @return The holding response or 204 if not found.
     */
    @GetMapping("/holdings/{stockId}")
    public ResponseEntity<HoldingsResponse> getHoldingByStock(
            Authentication authentication,
            @PathVariable Long stockId) {
        String email = authentication.getName();
        HoldingsResponse holding = holdingsService.getHoldingByStock(email, stockId);
        if (holding == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(holding);
    }

    /**
     * Cancels a pending order.
     *
     * @param authentication The authenticated user.
     * @param orderId        The order ID to cancel.
     * @return The cancelled order response.
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        String email = authentication.getName();
        OrderResponse response = orderService.cancelOrder(email, orderId);
        return ResponseEntity.ok(response);
    }
}

