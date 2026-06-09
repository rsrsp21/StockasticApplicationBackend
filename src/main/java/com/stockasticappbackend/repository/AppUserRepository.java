package com.stockasticappbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserRole;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.projection.AdminUserProjection;

/**
 * Repository interface for AppUser entity operations.
 * Provides CRUD operations and custom query methods for user data.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param email The email address to search for.
     * @return An Optional containing the AppUser if found.
     */
    @Query(value = "SELECT * FROM app_user WHERE email = :email", nativeQuery = true)
    Optional<AppUser> findByEmail(@Param("email") String email);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email The email address to check.
     * @return true if a user exists with this email, false otherwise.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM app_user WHERE email = :email", nativeQuery = true)
    Integer existsByEmailNative(@Param("email") String email);

    default boolean existsByEmail(String email) {
        return existsByEmailNative(email) == 1;
    }

    /**
     * Finds all users with a specific status.
     *
     * @param status The user status to filter by (as string for native query).
     * @return A list of users matching the status.
     */
    @Query(value = "SELECT * FROM app_user WHERE user_status = :status", nativeQuery = true)
    List<AppUser> findByUserStatusString(@Param("status") String status);

    /**
     * Finds all users with a specific status.
     *
     * @param status The user status to filter by.
     * @return A list of users matching the status.
     */
    default List<AppUser> findByUserStatus(UserStatus status) {
        return findByUserStatusString(status.name());
    }

    /**
     * Count users by role (as string for native query).
     * 
     * @param role The role to count (as string)
     * @return Number of users with the specified role
     */
    @Query(value = "SELECT COUNT(*) FROM app_user WHERE role = :role", nativeQuery = true)
    long countByRoleString(@Param("role") String role);

    /**
     * Count users by role (e.g., count only USER role, excluding ADMIN)
     * 
     * @param role The role to count
     * @return Number of users with the specified role
     */
    default long countByRole(UserRole role) {
        return countByRoleString(role.name());
    }

    /**
     * Fetches all users as lightweight projections.
     * Uses native query to bypass JPA entity loading entirely.
     */
    @Query(value = """
        SELECT user_id AS userId, name, email, role, user_status AS userStatus, mobile, created_at AS createdAt
        FROM app_user
        WHERE role = 'USER'
        ORDER BY user_id
        """, nativeQuery = true)
    List<AdminUserProjection> findAllUsersProjection();

    // ── Keyset pagination for Admin Manage Users ──

    /**
     * Fetch users with keyset pagination (cursor-based).
     * Filters by search term (name/email) and optional status.
     * Use Pageable to control page size so Spring Data applies dialect-specific pagination safely.
     */
    @Query(value = """
        SELECT * FROM app_user 
        WHERE user_id > :cursorId 
          AND role = 'USER'
          AND (:search IS NULL OR name LIKE CONCAT('%', :search, '%') OR email LIKE CONCAT('%', :search, '%'))
          AND (:status IS NULL OR user_status = :status)
        ORDER BY user_id ASC
        """, nativeQuery = true)
    List<AppUser> findUsersKeyset(
            @Param("cursorId") Long cursorId,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Fetch users with offset pagination and backend sorting.
     */
    @Query(value = """
        SELECT * FROM app_user
        WHERE role = 'USER'
          AND (:search IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR user_status = :status)
        """,
        countQuery = """
        SELECT COUNT(*) FROM app_user
        WHERE role = 'USER'
          AND (:search IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR user_status = :status)
        """,
        nativeQuery = true)
    Page<AppUser> findUsersPaged(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Count total matching users for keyset pagination metadata.
     */
    @Query(value = """
        SELECT COUNT(*) FROM app_user 
        WHERE role = 'USER'
          AND (:search IS NULL OR name LIKE CONCAT('%', :search, '%') OR email LIKE CONCAT('%', :search, '%'))
          AND (:status IS NULL OR user_status = :status)
        """, nativeQuery = true)
    long countUsersFiltered(
            @Param("search") String search,
            @Param("status") String status
    );
}
