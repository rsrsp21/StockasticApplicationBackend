package com.stockasticappbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Stockastic Application Backend.
 * 
 * This Spring Boot application provides REST APIs and WebSocket endpoints
 * for managing stocks, stock prices, user authentication, and KYC processing.
 *
 * @author Stockastic Team
 * @version 3.0
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class StockasticApplicationBackendApplication {

	/**
	 * Application entry point.
	 *
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(StockasticApplicationBackendApplication.class, args);
	}

}
