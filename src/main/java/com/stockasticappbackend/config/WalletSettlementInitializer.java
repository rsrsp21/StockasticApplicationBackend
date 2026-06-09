package com.stockasticappbackend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.stockasticappbackend.service.wallet.FundSettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes wallet fund settlement checks on application startup.
 * Runs independently from stock price initialization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletSettlementInitializer implements ApplicationRunner {

    private final FundSettlementService fundSettlementService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Startup - Checking pending wallet fund settlements...");
        try {
            int settledCount = fundSettlementService.processSettlements();
            if (settledCount > 0) {
                log.info("Startup wallet settlement completed. {} orders settled.", settledCount);
            } else {
                log.info("Startup wallet settlement: no eligible orders.");
            }
        } catch (Exception e) {
            log.error("Error during startup wallet fund settlement processing", e);
        }
    }
}
