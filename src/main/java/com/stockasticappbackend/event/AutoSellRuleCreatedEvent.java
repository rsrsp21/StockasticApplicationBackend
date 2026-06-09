package com.stockasticappbackend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AutoSellRuleCreatedEvent extends ApplicationEvent {
    private final Long stockId;

    public AutoSellRuleCreatedEvent(Object source, Long stockId) {
        super(source);
        this.stockId = stockId;
    }
}
