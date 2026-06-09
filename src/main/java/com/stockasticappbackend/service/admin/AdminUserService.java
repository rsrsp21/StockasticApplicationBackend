package com.stockasticappbackend.service.admin;

import java.util.List;

import com.stockasticappbackend.dto.user.AdminUserPageResponse;
import com.stockasticappbackend.dto.user.AdminUserResponse;

/**
 * Service interface for administrative user management operations.
 */
public interface AdminUserService {

    /**
     * Retrieves a list of all users in the system.
     *
     * @return A list of AdminUserResponse objects.
     */
    List<AdminUserResponse> getAllUsers();

    /**
     * Retrieves users with backend pagination and sorting.
     *
     * @param page     Zero-based page index.
     * @param search   Optional search term for name/email.
     * @param status   Optional status filter (ACTIVE, BLOCKED, etc.).
     * @param sortBy   Sort field.
     * @param sortDir  Sort direction (asc/desc).
     * @param pageSize Number of users per page.
     * @return Paginated response with users and cursor info.
     */
    AdminUserPageResponse getUsersPaged(int page, String search, String status, String sortBy, String sortDir, int pageSize);

    /**
     * Blocks a user account.
     *
     * @param userId The ID of the user to block.
     */
    void blockUser(Long userId);

    /**
     * Unblocks a user account.
     *
     * @param userId The ID of the user to unblock.
     */
    void unblockUser(Long userId);

    /**
     * Permanently deletes a user and all mapped dependent data.
     *
     * @param userId The ID of the user to delete.
     */
    void deleteUserPermanently(Long userId);
}
