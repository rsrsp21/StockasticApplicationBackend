package com.stockasticappbackend.dto.wallet;

import com.stockasticappbackend.util.Constants;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending OTP.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    /** Purpose of the OTP (ADD_FUNDS, WITHDRAW, LINK_BANK). */
    @NotBlank(message = Constants.PURPOSE_REQUIRED)
    private String purpose;

    /** Transaction amount (for ADD_FUNDS/WITHDRAW operations). */
    private BigDecimal amount;
}


