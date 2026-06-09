package com.stockasticappbackend.repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for KYC entity operations.
 * Provides CRUD operations and custom query methods for KYC data.
 */
@Repository
public interface KycRepository extends JpaRepository<Kyc, Long> {

    /**
     * Finds a KYC record by the associated user.
     *
     * @param userId The user's ID.
     * @return An Optional containing the Kyc if found.
     */
    @Query(value = "SELECT * FROM kyc WHERE user_id = :userId", nativeQuery = true)
    Optional<Kyc> findByUserId(@Param("userId") Long userId);

    /**
     * Finds a KYC record by the associated user entity.
     *
     * @param user The AppUser entity.
     * @return An Optional containing the Kyc if found.
     */
    default Optional<Kyc> findByUser(AppUser user) {
        return findByUserId(user.getUserId());
    }

    /**
     * Finds a KYC record by user ID (alternate method name for compatibility).
     *
     * @param userId The user's ID.
     * @return An Optional containing the Kyc if found.
     */
    @Query(value = "SELECT * FROM kyc WHERE user_id = :userId", nativeQuery = true)
    Optional<Kyc> findByUser_UserId(@Param("userId") Long userId);

    /**
     * Checks if a KYC record exists for the given user.
     *
     * @param userId The user's ID.
     * @return true if a KYC record exists, false otherwise.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM kyc WHERE user_id = :userId", nativeQuery = true)
    Integer existsByUserIdNative(@Param("userId") Long userId);

    default boolean existsByUserId(Long userId) {
        return existsByUserIdNative(userId) == 1;
    }

    /**
     * Checks if a KYC record exists for the given user entity.
     *
     * @param user The AppUser entity.
     * @return true if a KYC record exists, false otherwise.
     */
    default boolean existsByUser(AppUser user) {
        return existsByUserId(user.getUserId());
    }

    /**
     * Finds all KYC records with a specific status (native query).
     *
     * @param status The KYC status to filter by (as string).
     * @return A list of Kyc entities matching the status.
     */
    @Query(value = "SELECT * FROM kyc WHERE kyc_status = :status", nativeQuery = true)
    List<Kyc> findByKycStatusString(@Param("status") String status);

    /**
     * Finds all KYC records with a specific status.
     *
     * @param status The KYC status to filter by.
     * @return A list of Kyc entities matching the status.
     */
    default List<Kyc> findByKycStatus(KycStatus status) {
        return findByKycStatusString(status.name());
    }

    /**
     * Finds KYC records with a specific status (paginated, native query).
     *
     * @param status   The KYC status to filter by (as string).
     * @param pageable Pagination parameters.
     * @return A page of Kyc entities matching the status.
     */
    @Query(value = "SELECT * FROM kyc WHERE kyc_status = :status", countQuery = "SELECT COUNT(*) FROM kyc WHERE kyc_status = :status", nativeQuery = true)
    Page<Kyc> findByKycStatusPagedString(@Param("status") String status, Pageable pageable);

    /**
     * Finds KYC records with a specific status (paginated).
     *
     * @param status   The KYC status to filter by.
     * @param pageable Pagination parameters.
     * @return A page of Kyc entities matching the status.
     */
    default Page<Kyc> findByKycStatus(KycStatus status, Pageable pageable) {
        return findByKycStatusPagedString(status.name(), pageable);
    }

    /**
     * Counts KYC records with a specific status (native query).
     *
     * @param status The KYC status to count (as string).
     * @return The number of KYC records with the given status.
     */
    @Query(value = "SELECT COUNT(*) FROM kyc WHERE kyc_status = :status", nativeQuery = true)
    long countByKycStatusString(@Param("status") String status);

    /**
     * Counts KYC records with a specific status.
     *
     * @param status The KYC status to count.
     * @return The number of KYC records with the given status.
     */
    default long countByKycStatus(KycStatus status) {
        return countByKycStatusString(status.name());
    }
}