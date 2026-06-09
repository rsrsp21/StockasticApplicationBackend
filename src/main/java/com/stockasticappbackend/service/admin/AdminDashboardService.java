package com.stockasticappbackend.service.admin;

import com.stockasticappbackend.dto.admin.AdminDashboardStatsResponse;

/**
 * Service interface for admin dashboard operations.
 */
public interface AdminDashboardService {

    /**
     * Retrieves aggregated statistics for the admin dashboard.
     *
     * @return AdminDashboardStatsResponse containing current system stats.
     */
    AdminDashboardStatsResponse getDashboardStats();
}
