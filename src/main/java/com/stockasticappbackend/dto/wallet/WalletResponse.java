package com.stockasticappbackend.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for wallet information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    /** Unique identifier for the wallet. */
    private Long walletId;

    /** User ID who owns this wallet. */
    private Long userId;

    /** Funds available for withdrawal or new trades. */
    private BigDecimal availableBalance;

    /** Funds locked in pending buy orders. */
    private BigDecimal lockedBalance;

    /** Total wallet value (available + locked). */
    private BigDecimal totalBalance;

    /** Currency code. */
    private String currency;

    /** Timestamp when the wallet was last updated. */
    private LocalDateTime updatedAt;
}
