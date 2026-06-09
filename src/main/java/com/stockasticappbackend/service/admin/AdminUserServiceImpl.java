package com.stockasticappbackend.service.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.AdminUserMapper;
import com.stockasticappbackend.dto.user.AdminUserPageResponse;
import com.stockasticappbackend.dto.user.AdminUserResponse;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserRole;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.projection.AdminUserProjection;
import com.stockasticappbackend.security.service.RefreshTokenService;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of AdminUserService for user management operations.
 * Provides administrative functions for managing user accounts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final AppUserRepository appUserRepository;
    private final AdminUserMapper mapper;
    private final RefreshTokenService refreshTokenService;

    /**
     * Retrieves all users using a lightweight projection (no JPA N+1 problem).
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return appUserRepository.findAllUsersProjection().stream()
                .map(p -> {
                    AdminUserResponse dto = new AdminUserResponse();
                    dto.setUserId(p.getUserId());
                    dto.setName(p.getName());
                    dto.setEmail(p.getEmail());
                    dto.setRole(p.getRole());
                    dto.setStatus(p.getUserStatus());
                    dto.setMobile(p.getMobile());
                    dto.setCreatedAt(p.getCreatedAt());
                    return dto;
                })
                .toList();
    }

    /**
     * Retrieves users with backend pagination and sorting.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminUserPageResponse getUsersPaged(int page, String search, String status, String sortBy, String sortDir, int pageSize) {
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        String statusParam = null;
        if (normalizedStatus != null) {
            try {
                UserStatus.valueOf(normalizedStatus);
                statusParam = normalizedStatus;
            } catch (IllegalArgumentException ignored) {
                statusParam = null;
            }
        }

        String mappedSortField = mapSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, mappedSortField);
        if (!"user_id".equals(mappedSortField)) {
            sort = sort.and(Sort.by(direction, "user_id"));
        }

        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, pageSize, sort);
        Page<AppUser> userPage = appUserRepository.findUsersPaged(searchParam, statusParam, pageable);

        List<AdminUserResponse> responseList = mapper.toResponseList(userPage.getContent());
        boolean hasMore = userPage.hasNext();
        Long nextCursor = null;

        return new AdminUserPageResponse(responseList, nextCursor, hasMore, userPage.getTotalElements(), userPage.getSize());
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "created_at";
        }

        return switch (sortBy) {
            case "name" -> "name";
            case "mobile" -> "mobile";
            case "status" -> "user_status";
            case "createdAt" -> "created_at";
            case "userId", "id" -> "user_id";
            default -> "created_at";
        };
    }

    /**
     * Blocks a user by their ID.
     *
     * @throws ResourceNotFoundException If the user is not found.
     */
    @Override
    public void blockUser(Long userId) {
        AppUser user = findUser(userId);
        user.setUserStatus(UserStatus.BLOCKED);
        appUserRepository.save(user);
        refreshTokenService.deleteByUserId(userId);
    }

    /**
     * Unblocks a user by their ID.
     *
     * @throws ResourceNotFoundException If the user is not found.
     */
    @Override
    public void unblockUser(Long userId) {
        AppUser user = findUser(userId);
        user.setUserStatus(UserStatus.ACTIVE);
        appUserRepository.save(user);
    }

    /**
     * Permanently deletes a user and all mapped dependent data.
     * Uses JPA cascading from AppUser relations (including activity logs).
     */
    @Override
    public void deleteUserPermanently(Long userId) {
        AppUser user = findUser(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin users cannot be permanently deleted");
        }

        // Defensive cleanup for auth sessions before entity delete.
        refreshTokenService.deleteByUserId(userId);
        appUserRepository.delete(user);
    }

    /**
     * Finds a user by ID.
     *
     * @param id The user's ID.
     * @return The AppUser entity.
     * @throws ResourceNotFoundException If the user is not found.
     */
    private AppUser findUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));
    }
}
