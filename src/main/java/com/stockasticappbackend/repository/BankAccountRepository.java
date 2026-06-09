package com.stockasticappbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.BankAccount;

/**
 * Repository for BankAccount entity operations.
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * Find all bank accounts by user ID.
     *
     * @param userId The user's ID.
     * @return List of bank accounts.
     */
    @Query(value = "SELECT * FROM bank_accounts WHERE user_id = :userId", nativeQuery = true)
    List<BankAccount> findByUserId(@Param("userId") Long userId);

    /**
     * Find all bank accounts for a user.
     *
     * @param user The user entity.
     * @return List of bank accounts.
     */
    default List<BankAccount> findByUser(AppUser user) {
        return findByUserId(user.getUserId());
    }

    /**
     * Find all bank accounts by user ID (alias for findByUserId).
     *
     * @param userId The user's ID.
     * @return List of bank accounts.
     */
    default List<BankAccount> findByUserUserId(Long userId) {
        return findByUserId(userId);
    }

    /**
     * Find the first bank account for a user (primary account) by user ID.
     *
     * @param userId The user's ID.
     * @return Optional containing the first bank account if found.
     */
    @Query(value = "SELECT * FROM bank_accounts WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<BankAccount> findFirstByUserId(@Param("userId") Long userId);

    /**
     * Find the first bank account for a user (primary account).
     *
     * @param user The user entity.
     * @return Optional containing the first bank account if found.
     */
    default Optional<BankAccount> findFirstByUser(AppUser user) {
        return findFirstByUserId(user.getUserId());
    }

    /**
     * Check if a user has already linked a specific account number (native).
     *
     * @param userId        The user's ID.
     * @param accountNumber The account number to check.
     * @return 1 if exists, 0 otherwise.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM bank_accounts WHERE user_id = :userId AND account_number = :accountNumber", nativeQuery = true)
    Integer existsByUserIdAndAccountNumberNative(@Param("userId") Long userId, @Param("accountNumber") String accountNumber);

    /**
     * Check if a user has already linked a specific account number.
     *
     * @param user          The user entity.
     * @param accountNumber The account number to check.
     * @return True if the account number is already linked.
     */
    default boolean existsByUserAndAccountNumber(AppUser user, String accountNumber) {
        return existsByUserIdAndAccountNumberNative(user.getUserId(), accountNumber) == 1;
    }

    /**
     * Find bank account by ID and user ID (native).
     *
     * @param id     The bank account ID.
     * @param userId The user's ID.
     * @return Optional containing the bank account if found and owned by user.
     */
    @Query(value = "SELECT * FROM bank_accounts WHERE id = :id AND user_id = :userId", nativeQuery = true)
    Optional<BankAccount> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Find bank account by ID and user (for ownership verification).
     *
     * @param id   The bank account ID.
     * @param user The user entity.
     * @return Optional containing the bank account if found and owned by user.
     */
    default Optional<BankAccount> findByIdAndUser(Long id, AppUser user) {
        return findByIdAndUserId(id, user.getUserId());
    }

    /**
     * Find the primary bank account for a user by user ID (native).
     *
     * @param userId The user's ID.
     * @return Optional containing the primary bank account if found.
     */
    @Query(value = "SELECT * FROM bank_accounts WHERE user_id = :userId AND is_primary = true", nativeQuery = true)
    Optional<BankAccount> findByUserIdAndIsPrimaryTrue(@Param("userId") Long userId);

    /**
     * Find the primary bank account for a user.
     *
     * @param user The user entity.
     * @return Optional containing the primary bank account if found.
     */
    default Optional<BankAccount> findByUserAndIsPrimaryTrue(AppUser user) {
        return findByUserIdAndIsPrimaryTrue(user.getUserId());
    }
}
