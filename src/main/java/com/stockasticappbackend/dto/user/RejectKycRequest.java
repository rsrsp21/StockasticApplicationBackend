package com.stockasticappbackend.dto.user;

import com.stockasticappbackend.util.Constants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Request DTO for KYC rejection by admin.
 */
@Data
public class RejectKycRequest {

    /** The reason for rejecting the KYC submission. */
    @NotBlank(message = Constants.REJECTION_REASON_REQUIRED)
    @Size(min = 5, max = 255, message = Constants.REJECTION_REASON_RANGE_5_255)
    private String reason;
}


