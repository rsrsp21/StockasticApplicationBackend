package com.stockasticappbackend.repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Order;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.OrderStatus;

/**
 * Repository for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds all orders for a user ID, ordered by creation date descending (native).
     *
     * @param userId   The user's ID.
     * @param pageable Pagination information.
     * @return Page of orders.
     */
    @Query(value = "SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC", countQuery = "SELECT COUNT(*) FROM orders WHERE user_id = :userId", nativeQuery = true)
    Page<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds all orders for a user, ordered by creation date descending.
     *
     * @param user     The user entity.
     * @param pageable Pagination information.
     * @return Page of orders.
     */
    default Page<Order> findByUserOrderByCreatedAtDesc(AppUser user, Pageable pageable) {
        return findByUserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
    }

    /**
     * Finds all orders for a user ID with a specific status (native).
     *
     * @param userId The user's ID.
     * @param status The order status (as string).
     * @return List of orders.
     */
    @Query(value = "SELECT * FROM orders WHERE user_id = :userId AND status = :status", nativeQuery = true)
    List<Order> findByUserIdAndStatusString(@Param("userId") Long userId, @Param("status") String status);

    /**
     * Finds all orders for a user with a specific status.
     *
     * @param user   The user entity.
     * @param status The order status.
     * @return List of orders.
     */
    default List<Order> findByUserAndStatus(AppUser user, OrderStatus status) {
        return findByUserIdAndStatusString(user.getUserId(), status.name());
    }

    /**
     * Finds all orders for a user ID (no pagination) (native).
     *
     * @param userId The user's ID.
     * @return List of orders.
     */
    @Query(value = "SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC", nativeQuery = true)
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Finds all orders for a user (no pagination).
     *
     * @param user The user entity.
     * @return List of orders.
     */
    default List<Order> findByUserOrderByCreatedAtDesc(AppUser user) {
        return findByUserIdOrderByCreatedAtDesc(user.getUserId());
    }

    /**
     * Finds ALL orders with a specific status (native).
     *
     * @param status The order status (as string).
     * @return List of orders.
     */
    @Query(value = "SELECT * FROM orders WHERE status = :status", nativeQuery = true)
    List<Order> findByStatusString(@Param("status") String status);

    /**
     * Finds ALL orders with a specific status (Admin/Scheduler use).
     */
    default List<Order> findByStatus(OrderStatus status) {
        return findByStatusString(status.name());
    }
    /**
     * Finds the most traded stocks (by number of orders).
     *
     * @param pageable Pagination info (limit).
     * @return List of Stock objects.
     */
    @Query("SELECT o.stock FROM Order o GROUP BY o.stock ORDER BY COUNT(o) DESC")
    List<Stock> findMostTradedStocks(Pageable pageable);

    /**
     * Finds SELL orders that are eligible for T+1 settlement.
     * Criteria: SELL order, FILLED status, not yet settled, executed more than 24 hours ago.
     *
     * @param cutoffTime Orders executed before this time are eligible.
     * @return List of settleable sell orders.
     */
    @Query(value = "SELECT * FROM orders WHERE order_type = 'SELL' AND status = 'FILLED' " +
            "AND is_settled = false AND executed_at <= :cutoffTime", nativeQuery = true)
    List<Order> findSettleableSellOrders(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Locks a single order row for update to prevent concurrent settlement of the same order.
     */
    @Query(value = "SELECT * FROM orders WHERE order_id = :orderId FOR UPDATE", nativeQuery = true)
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);
}
