package com.stockasticappbackend.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.stockasticappbackend.dto.sip.SipResponse;
import com.stockasticappbackend.dto.sip.SipTransactionResponse;
import com.stockasticappbackend.model.entity.Sip;
import com.stockasticappbackend.model.entity.SipTransaction;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SipMapper {

    private final StockMapper stockMapper;

    public SipResponse toResponse(Sip sip) {
        return SipResponse.builder()
                .id(sip.getId())
                .stock(stockMapper.toResponse(sip.getStock()))
                .frequency(sip.getFrequency())
                .quantity(sip.getQuantity())
                .startDate(sip.getStartDate())
                .nextExecutionDate(sip.getNextExecutionDate())
                .status(sip.getStatus())
                .createdDate(sip.getCreatedDate())
                .build();
    }

    public List<SipResponse> toResponseList(List<Sip> sips) {
        return sips.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public SipTransactionResponse toTransactionResponse(SipTransaction transaction) {
        return SipTransactionResponse.builder()
                .id(transaction.getId())
                .sipId(transaction.getSip().getId())
                .stockSymbol(transaction.getSip().getStock().getSymbol())
                .orderId(transaction.getOrder() != null ? transaction.getOrder().getOrderId() : null)
                .executionDate(transaction.getExecutionDate())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .price(transaction.getPrice())
                .quantity(transaction.getQuantity())
                .build();
    }
}
