package com.stockasticappbackend.dto.wallet;

import com.stockasticappbackend.util.Constants;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding funds to wallet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFundsRequest {

    /** Amount to add to wallet. */
    @NotNull(message = Constants.AMOUNT_REQUIRED)
    @DecimalMin(value = "1.00", message = Constants.MIN_DEPOSIT_AMOUNT_1)
    private BigDecimal amount;

    /** Optional description for the transaction. */
    @Size(max = 255, message = Constants.DESCRIPTION_MAX_255)
    private String description;

    /** Payment method used (UPI, CARD, NETBANKING). */
    @NotNull(message = Constants.PAYMENT_METHOD_REQUIRED)
    private String paymentMethod;

    /** OTP for verification. */
    @NotNull(message = Constants.OTP_REQUIRED)
    @Size(min = 4, max = 4, message = Constants.OTP_4_DIGITS)
    private String otp;
}


