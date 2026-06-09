package com.stockasticappbackend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PriceAlertCreatedEvent extends ApplicationEvent {
    private final Long stockId;

    public PriceAlertCreatedEvent(Object source, Long stockId) {
        super(source);
        this.stockId = stockId;
    }
}
