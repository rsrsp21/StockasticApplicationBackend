package com.stockasticappbackend.dto.admin;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for aggregated admin dashboard statistics.
 */
@Data
@Builder
public class AdminDashboardStatsResponse {

    /** Total number of registered users. */
    private long totalUsers;

    /** Number of users with approved KYC. */
    private long kycApproved;

    /** Number of users with rejected KYC. */
    private long kycRejected;

    /** Number of users with pending KYC. */
    private long kycPending;

    /** Total number of stocks in the system. */
    private long totalStocks;
}
