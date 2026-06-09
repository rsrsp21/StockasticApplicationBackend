package com.stockasticappbackend.dto.wallet;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for linking a new bank account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkBankAccountRequest {

    /** Bank name. */
    @NotBlank(message = Constants.BANK_NAME_REQUIRED)
    @Size(max = 100, message = Constants.BANK_NAME_MAX_100)
    private String bankName;

    /** Account number. */
    @NotBlank(message = Constants.ACCOUNT_NUMBER_REQUIRED)
    @Size(min = 9, max = 18, message = Constants.ACCOUNT_NUMBER_RANGE_9_18)
    @Pattern(regexp = "^[0-9]+$", message = Constants.ACCOUNT_NUMBER_DIGITS_ONLY)
    private String accountNumber;

    /** IFSC code. */
    @NotBlank(message = Constants.IFSC_REQUIRED)
    @Size(min = 11, max = 11, message = Constants.IFSC_11_CHARS)
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = Constants.IFSC_FORMAT_INVALID)
    private String ifscCode;

    /** Account holder name. */
    @NotBlank(message = Constants.ACCOUNT_HOLDER_NAME_REQUIRED)
    @Size(max = 100, message = Constants.HOLDER_NAME_MAX_100)
    private String holderName;
}


