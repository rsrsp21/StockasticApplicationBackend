package com.stockasticappbackend.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoSellRuleResponse {
    private Long ruleId;
    private Long stockId;
    private String symbol;
    private String stockName;
    private BigDecimal targetPrice;
    private BigDecimal stopLoss;
    private Integer quantity;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
}
