package com.stockasticappbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@Configuration
public class WebSocketSchedulerConfig {

    @Bean(name = "webSocketTaskScheduler")
    public TaskScheduler webSocketTaskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setThreadNamePrefix("ws-price-");
        scheduler.setVirtualThreads(true);
        return scheduler;
    }
}
