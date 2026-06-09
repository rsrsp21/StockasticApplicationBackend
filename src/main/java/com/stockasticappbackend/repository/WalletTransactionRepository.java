package com.stockasticappbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.entity.WalletTransaction;

/**
 * Repository for WalletTransaction entity operations.
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Find all transactions for a wallet ID, ordered by creation date descending (native).
     *
     * @param walletId The wallet ID.
     * @return List of transactions.
     */
    @Query(value = "SELECT * FROM wallet_transactions WHERE wallet_id = :walletId ORDER BY created_at DESC", nativeQuery = true)
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId);

    /**
     * Find all transactions for a wallet, ordered by creation date descending.
     *
     * @param wallet The wallet entity.
     * @return List of transactions.
     */
    default List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet) {
        return findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId());
    }

    /**
     * Find paginated transactions for a wallet by wallet ID (native).
     *
     * @param walletId The wallet's ID.
     * @param pageable Pagination parameters.
     * @return Page of transactions.
     */
    @Query(value = "SELECT * FROM wallet_transactions WHERE wallet_id = :walletId ORDER BY created_at DESC", countQuery = "SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = :walletId", nativeQuery = true)
    Page<WalletTransaction> findByWalletWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId, Pageable pageable);

    /**
     * Find transaction by reference ID (native).
     *
     * @param referenceId The external reference ID.
     * @return Optional containing the transaction if found.
     */
    @Query(value = "SELECT * FROM wallet_transactions WHERE reference_id = :referenceId", nativeQuery = true)
    Optional<WalletTransaction> findByReferenceId(@Param("referenceId") String referenceId);
}
