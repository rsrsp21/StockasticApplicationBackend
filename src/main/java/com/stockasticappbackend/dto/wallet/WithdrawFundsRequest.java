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
 * Request DTO for withdrawing funds from wallet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawFundsRequest {

    /** Amount to withdraw from wallet. */
    @NotNull(message = Constants.AMOUNT_REQUIRED)
    @DecimalMin(value = "1.00", message = Constants.MIN_WITHDRAWAL_AMOUNT_1)
    private BigDecimal amount;

    /** Bank account ID to withdraw to. */
    @NotNull(message = Constants.BANK_ACCOUNT_ID_REQUIRED)
    private Long bankAccountId;

    /** OTP for verification. */
    @NotNull(message = Constants.OTP_REQUIRED)
    @Size(min = 4, max = 4, message = Constants.OTP_4_DIGITS)
    private String otp;
}


