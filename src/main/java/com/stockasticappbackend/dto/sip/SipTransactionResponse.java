package com.stockasticappbackend.dto.sip;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockasticappbackend.model.enums.SipTransactionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SipTransactionResponse {
    private Long id;
    private Long sipId;
    private String stockSymbol;
    private Long orderId;
    private LocalDateTime executionDate;
    private SipTransactionStatus status;
    private String failureReason;
    private BigDecimal price;
    private Integer quantity;
}
