package com.stockasticappbackend.dto.order;

import com.stockasticappbackend.util.Constants;

import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for placing an order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /** ID of the stock to trade. */
    @NotNull(message = Constants.STOCK_ID_REQUIRED)
    private Long stockId;

    /** Type of order (BUY or SELL). */
    @NotNull(message = Constants.ORDER_TYPE_REQUIRED)
    private OrderType orderType;

    /** Mode of order (MARKET or LIMIT). */
    @NotNull(message = Constants.ORDER_MODE_REQUIRED)
    private OrderMode orderMode;

    /** Quantity of shares to trade. */
    @NotNull(message = Constants.QUANTITY_REQUIRED)
    @Min(value = 1, message = Constants.QUANTITY_MIN_ONE)
    private Integer quantity;

    /** Price per share (required for LIMIT orders). */
    private BigDecimal price;
}


