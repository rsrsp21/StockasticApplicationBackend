package com.stockasticappbackend.dto.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.stockasticappbackend.model.enums.PriceAlertCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertResponse {
    private Long alertId;
    private Long stockId;
    private String symbol;
    private String stockName;
    private BigDecimal targetPrice;
    private PriceAlertCondition condition;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
}
