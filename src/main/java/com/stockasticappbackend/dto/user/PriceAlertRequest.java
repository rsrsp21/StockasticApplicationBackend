package com.stockasticappbackend.dto.user;

import java.math.BigDecimal;

import com.stockasticappbackend.model.enums.PriceAlertCondition;

import lombok.Data;

@Data
public class PriceAlertRequest {
    private Long stockId;
    private BigDecimal targetPrice;
    private PriceAlertCondition condition;
}
