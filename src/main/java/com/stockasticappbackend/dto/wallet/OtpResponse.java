package com.stockasticappbackend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {

    /** Whether the operation was successful. */
    private boolean success;

    /** Message to display to user. */
    private String message;

    /** Expiry time in seconds. */
    private Integer expirySeconds;
}
