package com.stockasticappbackend.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockasticappbackend.model.enums.TransactionStatus;
import com.stockasticappbackend.model.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for wallet transaction information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {

    /** Unique identifier for the transaction. */
    private Long transactionId;

    /** Transaction amount. */
    private BigDecimal amount;

    /** Type of transaction (CREDIT or DEBIT). */
    private TransactionType type;

    /** Status of the transaction. */
    private TransactionStatus status;

    /** External reference ID. */
    private String referenceId;

    /** User-facing description. */
    private String description;

    /** Timestamp when the transaction was created. */
    private LocalDateTime createdAt;
}
