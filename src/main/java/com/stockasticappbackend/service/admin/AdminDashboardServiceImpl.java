package com.stockasticappbackend.service.admin;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.admin.AdminDashboardStatsResponse;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.UserRole;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.repository.StockRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of AdminDashboardService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AppUserRepository appUserRepository;
    private final KycRepository kycRepository;
    private final StockRepository stockRepository;

    @Override
    @Cacheable("dashboardStats")
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = appUserRepository.countByRole(UserRole.USER);
        
        long kycApproved = kycRepository.countByKycStatus(KycStatus.APPROVED);
        long kycRejected = kycRepository.countByKycStatus(KycStatus.REJECTED);
        long kycPending = kycRepository.countByKycStatus(KycStatus.PENDING);
        long totalStocks = stockRepository.count();

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .kycApproved(kycApproved)
                .kycRejected(kycRejected)
                .kycPending(kycPending)
                .totalStocks(totalStocks)
                .build();
    }
}