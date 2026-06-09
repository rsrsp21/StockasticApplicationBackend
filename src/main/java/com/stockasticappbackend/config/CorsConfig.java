package com.stockasticappbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/*
 * Configuration class for Cross-Origin Resource Sharing (CORS) settings.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Default development origins
        List<String> defaultOrigins = Arrays.asList(
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*",
                "https://127.0.0.1:*",
                "https://*.inc1.devtunnels.ms"
        );

        // Add production origins from environment variable if provided
        if (corsAllowedOrigins != null && !corsAllowedOrigins.trim().isEmpty()) {
            String[] productionOrigins = corsAllowedOrigins.split(",");
            for (String origin : productionOrigins) {
                defaultOrigins.add(origin.trim());
            }
        }

        config.setAllowedOriginPatterns(defaultOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
