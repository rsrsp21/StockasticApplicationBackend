package com.stockasticappbackend.dto.sip;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.model.enums.SipFrequency;
import com.stockasticappbackend.model.enums.SipStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SipResponse {
    private Long id;
    private StockResponse stock;
    private SipFrequency frequency;
    private Integer quantity;
    private LocalDate startDate;
    private LocalDate nextExecutionDate;
    private SipStatus status;
    private LocalDateTime createdDate;
}
