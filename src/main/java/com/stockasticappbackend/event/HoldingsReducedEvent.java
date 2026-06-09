package com.stockasticappbackend.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class HoldingsReducedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long stockId;
    private final int remainingQuantity;

    public HoldingsReducedEvent(Object source, Long userId, Long stockId, int remainingQuantity) {
        super(source);
        this.userId = userId;
        this.stockId = stockId;
        this.remainingQuantity = remainingQuantity;
    }
}
