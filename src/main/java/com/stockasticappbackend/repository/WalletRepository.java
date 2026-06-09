package com.stockasticappbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Wallet;

/**
 * Repository for Wallet entity operations.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Find wallet by user ID (native).
     *
     * @param userId The user's ID.
     * @return Optional containing the wallet if found.
     */
    @Query(value = "SELECT * FROM wallets WHERE user_id = :userId", nativeQuery = true)
    Optional<Wallet> findByUserId(@Param("userId") Long userId);

    /**
     * Locks the wallet row for update to keep balance movement atomic under concurrency.
     */
    @Query(value = "SELECT * FROM wallets WHERE user_id = :userId FOR UPDATE", nativeQuery = true)
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Find wallet by user.
     *
     * @param user The user entity.
     * @return Optional containing the wallet if found.
     */
    default Optional<Wallet> findByUser(AppUser user) {
        return findByUserId(user.getUserId());
    }

    /**
     * Find wallet by user ID (alias for findByUserId).
     *
     * @param userId The user's ID.
     * @return Optional containing the wallet if found.
     */
    default Optional<Wallet> findByUserUserId(Long userId) {
        return findByUserId(userId);
    }

    /**
     * Check if a wallet exists for a user ID (native).
     *
     * @param userId The user's ID.
     * @return 1 if wallet exists, 0 otherwise.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM wallets WHERE user_id = :userId", nativeQuery = true)
    Integer existsByUserIdNative(@Param("userId") Long userId);

    /**
     * Check if a wallet exists for a user.
     *
     * @param user The user entity.
     * @return True if wallet exists.
     */
    default boolean existsByUser(AppUser user) {
        return existsByUserIdNative(user.getUserId()) == 1;
    }
}
