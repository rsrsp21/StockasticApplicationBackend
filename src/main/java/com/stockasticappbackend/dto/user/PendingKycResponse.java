package com.stockasticappbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for pending KYC submissions in admin view.
 * Contains user and KYC information for administrative review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingKycResponse {

    /** The unique identifier of the user. */
    private Long userId;

    /** The user's display name. */
    private String name;

    /** The user's email address. */
    private String email;

    /** The path to the user's profile image. */
    private String profileImagePath;

    /** The user's PAN card number. */
    private String panNumber;

    /** The user's Aadhaar card number. */
    private String aadhaarNumber;

    /** The number of KYC submission attempts. */
    private Integer attemptCount;

    /** The timestamp when KYC was submitted. */
    private LocalDateTime submittedAt;
}
