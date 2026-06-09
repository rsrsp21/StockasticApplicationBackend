package com.stockasticappbackend.util;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Utility class for building standardized API responses.
 */
public class ResponseBuilder {

    /**
     * Builds a standardized error response map.
     *
     * @param message The error message.
     * @param status  The HTTP status.
     * @return A map containing error details.
     */
    public static Map<String, Object> buildErrorResponse(String message, HttpStatus status) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message);
    }
}
