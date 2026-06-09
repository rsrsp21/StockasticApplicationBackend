package com.stockasticappbackend.dto.user;

import com.stockasticappbackend.model.enums.KycStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for user's KYC verification status.
 */
@Data
public class UserKycStatusResponse {

    /** The current KYC status (PENDING, APPROVED, REJECTED, NOT_ATTEMPTED). */
    private KycStatus kycStatus;

    /** The number of KYC submission attempts. */
    private Integer attemptCount;

    /** The reason for rejection (if applicable). */
    private String rejectionReason;

    /** The timestamp when KYC was submitted. */
    private LocalDateTime submittedAt;
}