package com.stockasticappbackend.service.admin;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PendingKycResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.AdminKycMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.security.service.RefreshTokenService;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of AdminKycService for KYC management operations.
 * Handles KYC approval, rejection, and document retrieval for administrators.
 * Automatically blocks users after 3 failed KYC attempts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminKycServiceImpl implements AdminKycService {

    private final KycRepository kycRepository;
    private final AppUserRepository appUserRepository;
    private final AdminKycMapper mapper;
    private final RefreshTokenService refreshTokenService;

    @Value("${file.upload-kyc}")
    private String kycUploadDir;

    /**
     * Approves a user's KYC application.
     *
     * @throws ResourceNotFoundException If no KYC record exists for the user.
     * @throws IllegalStateException     If KYC has already been reviewed.
     */
    @Override
    public void approveKyc(Long userId) {

        Kyc kyc = getKyc(userId);

        if (kyc.getKycStatus() != KycStatus.PENDING) {
            throw new IllegalStateException(Constants.KYC_ALREADY_REVIEWED);
        }

        kyc.setKycStatus(KycStatus.APPROVED);
        kyc.setReviewedAt(LocalDateTime.now());
    }

    /**
     * Rejects a user's KYC application.
     * If the user has exceeded 3 attempts, their account will be blocked.
     *
     * @throws ResourceNotFoundException If no KYC record exists for the user.
     * @throws IllegalStateException     If KYC has already been reviewed.
     */
    @Override
    public void rejectKyc(Long userId, String reason) {

        Kyc kyc = getKyc(userId);

        if (kyc.getKycStatus() != KycStatus.PENDING) {
            throw new IllegalStateException(Constants.KYC_ALREADY_REVIEWED);
        }

        kyc.setKycStatus(KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setReviewedAt(LocalDateTime.now());

        if (kyc.getAttemptCount() >= 3) {
            AppUser user = kyc.getUser();
            user.setUserStatus(UserStatus.BLOCKED);
            appUserRepository.save(user);
            refreshTokenService.deleteByUserId(user.getUserId());
        }
    }

    /**
     * Retrieves a list of pending KYC applications.
     */
    @Override
    public PageResponse<PendingKycResponse> getPendingKycs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Kyc> kycPage = kycRepository.findByKycStatus(KycStatus.PENDING, pageable);

        List<PendingKycResponse> content = mapper.toResponseList(kycPage.getContent());

        return PageResponse.<PendingKycResponse>builder()
                .content(content)
                .page(kycPage.getNumber())
                .size(kycPage.getSize())
                .totalElements(kycPage.getTotalElements())
                .totalPages(kycPage.getTotalPages())
                .first(kycPage.isFirst())
                .last(kycPage.isLast())
                .hasNext(kycPage.hasNext())
                .hasPrevious(kycPage.hasPrevious())
                .build();
    }

    /**
     * Retrieves the KYC document for a user.
     *
     * @throws ResourceNotFoundException If no KYC record or document exists.
     * @throws RuntimeException          If there's an error loading the document.
     */
    @Override
    public Resource viewKycDocument(Long userId) {

        Kyc kyc = getKyc(userId);

        try {
            Path filePath = Paths.get(kycUploadDir)
                    .resolve(kyc.getDocumentPath())
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResourceNotFoundException(Constants.KYC_DOCUMENT_NOT_FOUND);
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new RuntimeException(Constants.KYC_DOCUMENT_ERROR, e);
        }
    }

    /**
     * Retrieves a KYC record by user ID.
     *
     * @param userId The user's ID.
     * @return The Kyc entity.
     * @throws ResourceNotFoundException If no KYC record exists.
     */
    private Kyc getKyc(Long userId) {
        return kycRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.KYC_NOT_FOUND));
    }
}
