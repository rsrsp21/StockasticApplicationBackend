package com.stockasticappbackend.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Holdings;
import com.stockasticappbackend.model.entity.Stock;

/**
 * Repository for Holdings entity operations.
 */
@Repository
public interface HoldingsRepository extends JpaRepository<Holdings, Long> {

    /**
     * Finds a holding by user ID and stock ID (native).
     *
     * @param userId  The user's ID.
     * @param stockId The stock's ID.
     * @return Optional containing the holding if found.
     */
    @Query(value = "SELECT * FROM holdings WHERE user_id = :userId AND stock_id = :stockId", nativeQuery = true)
    Optional<Holdings> findByUserIdAndStockId(@Param("userId") Long userId, @Param("stockId") Long stockId);

    /**
     * Finds a holding by user and stock.
     *
     * @param user  The user entity.
     * @param stock The stock entity.
     * @return Optional containing the holding if found.
     */
    default Optional<Holdings> findByUserAndStock(AppUser user, Stock stock) {
        return findByUserIdAndStockId(user.getUserId(), stock.getStockId());
    }

    /**
     * Finds all holdings for a user ID (native).
     *
     * @param userId The user's ID.
     * @return List of holdings.
     */
    @Query(value = "SELECT * FROM holdings WHERE user_id = :userId", nativeQuery = true)
    List<Holdings> findByUserId(@Param("userId") Long userId);

    /**
     * Finds all holdings for a user.
     *
     * @param user The user entity.
     * @return List of holdings.
     */
    default List<Holdings> findByUser(AppUser user) {
        return findByUserId(user.getUserId());
    }

    /**
     * Checks if a user has any holdings for a stock by IDs (native).
     *
     * @param userId  The user's ID.
     * @param stockId The stock's ID.
     * @return 1 if holding exists, 0 otherwise.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM holdings WHERE user_id = :userId AND stock_id = :stockId", nativeQuery = true)
    Integer existsByUserIdAndStockIdNative(@Param("userId") Long userId, @Param("stockId") Long stockId);

    /**
     * Checks if a user has any holdings for a stock.
     *
     * @param user  The user entity.
     * @param stock The stock entity.
     * @return true if holding exists.
     */
    default boolean existsByUserAndStock(AppUser user, Stock stock) {
        return existsByUserIdAndStockIdNative(user.getUserId(), stock.getStockId()) == 1;
    }
    /**
     * Finds a holding by user ID and stock ID with PESSIMISTIC_WRITE lock.
     * Prevents race conditions during critical updates (buy/sell).
     *
     * @param userId  The user's ID.
     * @param stockId The stock's ID.
     * @return Optional containing the holding if found.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holdings h WHERE h.user.userId = :userId AND h.stock.stockId = :stockId")
    Optional<Holdings> findByUserIdAndStockIdForUpdate(@Param("userId") Long userId, @Param("stockId") Long stockId);

    default Optional<Holdings> findByUserAndStockForUpdate(AppUser user, Stock stock) {
        return findByUserIdAndStockIdForUpdate(user.getUserId(), stock.getStockId());
    }
}
