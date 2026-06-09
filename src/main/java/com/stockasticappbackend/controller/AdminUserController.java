package com.stockasticappbackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.stockasticappbackend.dto.user.AdminUserPageResponse;
import com.stockasticappbackend.dto.user.AdminUserResponse;
import com.stockasticappbackend.service.admin.AdminUserService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for administrative user management.
 * Provides endpoints for listing users and managing user account status.
 * All endpoints require ADMIN role authentication.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Retrieves a list of all users in the system.
     *
     * @return List of AdminUserResponse objects.
     */
    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return adminUserService.getAllUsers();
    }

    /**
     * Retrieves users with backend pagination and sorting.
     *
     * @param page   Zero-based page index.
     * @param search Optional search term to filter by name or email.
     * @param status Optional status filter (ACTIVE, BLOCKED).
     * @param sortBy Sort field (name, mobile, status, createdAt, userId).
     * @param sortDir Sort direction (asc/desc).
     * @param size   Page size (default 20, max 100).
     * @return AdminUserPageResponse with users and pagination metadata.
     */
    @GetMapping("/paged")
    public AdminUserPageResponse listUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "20") int size) {
        // Clamp page size
        int pageSize = Math.min(Math.max(size, 1), 100);
        return adminUserService.getUsersPaged(page, search, status, sortBy, sortDir, pageSize);
    }

    /**
     * Blocks a user account, preventing them from logging in.
     *
     * @param id The ID of the user to block.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @PutMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable Long id) {
        adminUserService.blockUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unblocks a user account, restoring their access.
     *
     * @param id The ID of the user to unblock.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @PutMapping("/{id}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable Long id) {
        adminUserService.unblockUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes a user and all mapped dependent records.
     *
     * @param id The ID of the user to permanently delete.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermanently(@PathVariable Long id) {
        adminUserService.deleteUserPermanently(id);
        return ResponseEntity.noContent().build();
    }
}
