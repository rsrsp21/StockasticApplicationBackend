package com.stockasticappbackend.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.stockasticappbackend.model.enums.KycStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entity representing a KYC (Know Your Customer) submission.
 * Stores user identity verification documents and verification status.
 * Each user can have only one KYC record.
 */
@Data
@Entity
@Table(name = "kyc")
public class Kyc {

    /** The unique identifier of the KYC record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kyc_id")
    private Long kycId;

    /** The user associated with this KYC submission. */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    /** The user's PAN card number. */
    @Column(name = "pan_number", nullable = false)
    private String panNumber;

    /** The user's Aadhaar card number. */
    @Column(name = "aadhaar_number", nullable = false)
    private String aadhaarNumber;

    /** The relative path to the uploaded KYC document. */
    @Column(name = "document_path", nullable = false)
    private String documentPath;

    /** The current verification status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus;

    /** The number of submission attempts (max 3). */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    /** The reason for rejection (if applicable). */
    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** Timestamp when the KYC was submitted. */
    @Column(name = "submitted_at")
    @CreationTimestamp
    private LocalDateTime submittedAt;

    /** Timestamp when the KYC was last reviewed. */
    @Column(name = "reviewed_at")
    @UpdateTimestamp
    private LocalDateTime reviewedAt;
}
