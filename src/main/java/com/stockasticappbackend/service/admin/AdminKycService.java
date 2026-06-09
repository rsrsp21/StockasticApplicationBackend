package com.stockasticappbackend.service.admin;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PendingKycResponse;
import org.springframework.core.io.Resource;

/**
 * Service interface for administrative KYC operations.
 */
public interface AdminKycService {

    /**
     * Approves a user's KYC submission.
     *
     * @param userId The ID of the user whose KYC is being approved.
     */
    void approveKyc(Long userId);

    /**
     * Rejects a user's KYC submission with a reason.
     *
     * @param userId The ID of the user whose KYC is being rejected.
     * @param reason The reason for rejection.
     */
    void rejectKyc(Long userId, String reason);

    /**
     * Retrieves a paginated list of pending KYC submissions.
     *
     * @param page The page number (0-indexed).
     * @param size The number of records per page.
     * @return A PageResponse containing PendingKycResponse objects.
     */
    PageResponse<PendingKycResponse> getPendingKycs(int page, int size);

    /**
     * Retrieves the KYC document for a user.
     *
     * @param userId The ID of the user.
     * @return The document as a Resource.
     */
    Resource viewKycDocument(Long userId);
}