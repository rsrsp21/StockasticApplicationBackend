package com.stockasticappbackend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for bank account information.
 * Account number is masked for security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {

    /** Unique identifier for the bank account. */
    private Long id;

    /** User ID who owns this bank account. */
    private Long userId;

    /** Bank name. */
    private String bankName;

    /** Masked account number */
    private String maskedAccountNumber;

    /** IFSC code. */
    private String ifscCode;

    /** Account holder name. */
    private String holderName;

    /** Whether the bank account has been verified. */
    private Boolean isVerified;

    /** Whether this is the primary bank account. */
    private Boolean isPrimary;
}
