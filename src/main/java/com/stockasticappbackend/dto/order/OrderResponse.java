package com.stockasticappbackend.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderStatus;
import com.stockasticappbackend.model.enums.OrderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for order details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    /** Order identifier. */
    private Long orderId;

    /** Stock symbol. */
    private String symbol;

    /** Stock name. */
    private String stockName;

    /** Type of order (BUY or SELL). */
    private OrderType orderType;

    /** Mode of order (MARKET or LIMIT). */
    private OrderMode orderMode;

    /** Current status of the order. */
    private OrderStatus status;

    /** Quantity of shares traded. */
    private Integer quantity;

    /** Price per share. */
    private BigDecimal price;

    /** Total order value. */
    private BigDecimal totalAmount;

    /** Order creation timestamp. */
    private LocalDateTime createdAt;

    /** Order execution timestamp. */
    private LocalDateTime executedAt;

    /** Average price at which the order was filled */
    private BigDecimal averageFilledPrice;
}
