package com.stockasticappbackend.controller;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PendingKycResponse;
import com.stockasticappbackend.dto.user.RejectKycRequest;
import com.stockasticappbackend.service.admin.AdminKycService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for administrative KYC (Know Your Customer) management.
 * Provides endpoints for reviewing, approving, and rejecting user KYC
 * submissions.
 * All endpoints require ADMIN role authentication.
 */
@RestController
@RequestMapping("/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final AdminKycService adminKycService;

    /**
     * Approves a user's KYC submission.
     *
     * @param userId The ID of the user whose KYC is being approved.
     * @return ResponseEntity with HTTP status 204 (No Content) on success.
     */
    @PutMapping("/{userId}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long userId) {
        adminKycService.approveKyc(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Rejects a user's KYC submission with a reason.
     *
     * @param userId  The ID of the user whose KYC is being rejected.
     * @param request The rejection request containing the reason.
     * @return ResponseEntity with HTTP status 204 (No Content) on success.
     */
    @PutMapping("/{userId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long userId,
            @RequestBody @Valid RejectKycRequest request) {

        adminKycService.rejectKyc(userId, request.getReason());
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of pending KYC submissions.
     *
     * @param page Page number (0-indexed, default: 0).
     * @param size Number of records per page (default: 10).
     * @return ResponseEntity containing a PageResponse of PendingKycResponse
     *         objects.
     */
    @GetMapping("/pending")
    public ResponseEntity<PageResponse<PendingKycResponse>> getPendingKycs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminKycService.getPendingKycs(page, size));
    }

    /**
     * Retrieves and serves a user's KYC document for viewing.
     *
     * @param userId The ID of the user whose document is being viewed.
     * @return ResponseEntity containing the document as a Resource.
     * @throws IOException If the file cannot be read.
     */
    @GetMapping("/{userId}/document")
    public ResponseEntity<Resource> viewKycDocument(@PathVariable Long userId) throws IOException {

        Resource resource = adminKycService.viewKycDocument(userId);

        String contentType = Files.probeContentType(resource.getFile().toPath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
