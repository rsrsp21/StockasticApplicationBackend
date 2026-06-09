package com.stockasticappbackend.dto.order;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user's stock holdings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldingsResponse {

    /** Holding identifier. */
    private Long holdingId;

    /** Stock identifier. */
    private Long stockId;

    /** Stock symbol. */
    private String symbol;

    /** Stock name. */
    private String stockName;

    /** Stock logo/image filename. */
    private String image;

    /** Stock sector. */
    private String sector;

    /** Quantity of shares owned. */
    private Integer quantity;

    /** Average buy price. */
    private BigDecimal averagePrice;

    /** Current market price. */
    private BigDecimal currentPrice;

    /** Total invested amount (quantity × averagePrice). */
    private BigDecimal investedAmount;

    /** Current value (quantity × currentPrice). */
    private BigDecimal currentValue;

    /** Profit/Loss amount. */
    private BigDecimal profitLoss;

    /** Profit/Loss percentage. */
    private BigDecimal profitLossPercent;

    /** Day's price change amount. */
    private BigDecimal dayChange;

    /** Day's price change percentage. */
    private BigDecimal dayChangePercent;

    /** Realized Profit/Loss (lifetime). */
    private BigDecimal realizedPnl;

    /** Total Profit/Loss (Realized + Unrealized). */
    private BigDecimal totalPnl;
}
