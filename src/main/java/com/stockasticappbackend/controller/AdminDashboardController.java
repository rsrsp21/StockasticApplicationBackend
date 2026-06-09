package com.stockasticappbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.admin.AdminDashboardStatsResponse;
import com.stockasticappbackend.service.admin.AdminDashboardService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin dashboard statistics.
 * Provides endpoints for retrieving aggregated system statistics.
 */
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Retrieves the aggregated dashboard statistics.
     *
     * @return ResponseEntity containing the AdminDashboardStatsResponse.
     */
    @GetMapping
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }
}
