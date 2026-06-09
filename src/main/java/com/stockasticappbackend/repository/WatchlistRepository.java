package com.stockasticappbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Watchlist;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    /**
     * Finds all watchlists for a user ordered by creation date (newest first).
     *
     * @param userId The user's ID.
     * @return A list of Watchlist entities.
     */
    @Query(value = "SELECT * FROM watchlist WHERE user_id = :userId ORDER BY created_at DESC", nativeQuery = true)
    List<Watchlist> findByUserId(@Param("userId") Long userId);

    /**
     * Finds all watchlists for a user entity ordered by creation date.
     *
     * @param user The AppUser entity.
     * @return A list of Watchlist entities.
     */
    default List<Watchlist> findByUserOrderByCreatedAtDesc(AppUser user) {
        return findByUserId(user.getUserId());
    }

    /**
     * Finds all watchlists for a user (paginated, newest first).
     *
     * @param userId   The user's ID.
     * @param pageable Pagination parameters.
     * @return A page of Watchlist entities.
     */
    @Query(value = "SELECT * FROM watchlist WHERE user_id = :userId ORDER BY created_at DESC", countQuery = "SELECT COUNT(*) FROM watchlist WHERE user_id = :userId", nativeQuery = true)
    Page<Watchlist> findByUserIdPaged(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds all watchlists for a user entity (paginated).
     *
     * @param user     The AppUser entity.
     * @param pageable Pagination parameters.
     * @return A page of Watchlist entities.
     */
    default Page<Watchlist> findByUserOrderByCreatedAtDesc(AppUser user, Pageable pageable) {
        return findByUserIdPaged(user.getUserId(), pageable);
    }

    /**
     * Finds a watchlist by ID and user ID.
     *
     * @param watchlistId The watchlist's ID.
     * @param userId      The user's ID.
     * @return An Optional containing the Watchlist if found.
     */
    @Query(value = "SELECT * FROM watchlist WHERE watchlist_id = :watchlistId AND user_id = :userId", nativeQuery = true)
    Optional<Watchlist> findByWatchlistIdAndUserId(@Param("watchlistId") Long watchlistId,
            @Param("userId") Long userId);

    /**
     * Finds a watchlist by ID and user entity.
     *
     * @param watchlistId The watchlist's ID.
     * @param user        The AppUser entity.
     * @return An Optional containing the Watchlist if found.
     */
    default Optional<Watchlist> findByWatchlistIdAndUser(Long watchlistId, AppUser user) {
        return findByWatchlistIdAndUserId(watchlistId, user.getUserId());
    }

    /**
     * Checks if a watchlist exists for a user.
     *
     * @param watchlistId The watchlist's ID.
     * @param userId      The user's ID.
     * @return true if the watchlist exists for the user.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM watchlist WHERE watchlist_id = :watchlistId AND user_id = :userId", nativeQuery = true)
    Integer existsByWatchlistIdAndUserIdNative(@Param("watchlistId") Long watchlistId,
            @Param("userId") Long userId);

    default boolean existsByWatchlistIdAndUserId(Long watchlistId, Long userId) {
        return existsByWatchlistIdAndUserIdNative(watchlistId, userId) == 1;
    }

    /**
     * Counts watchlists for a user.
     *
     * @param userId The user's ID.
     * @return The number of watchlists.
     */
    @Query(value = "SELECT COUNT(*) FROM watchlist WHERE user_id = :userId", nativeQuery = true)
    long countByUserId(@Param("userId") Long userId);

    /**
     * Counts watchlists for a user entity.
     *
     * @param user The AppUser entity.
     * @return The number of watchlists.
     */
    default long countByUser(AppUser user) {
        return countByUserId(user.getUserId());
    }

    /**
     * Finds a watchlist by name and user ID.
     *
     * @param name   The watchlist name.
     * @param userId The user's ID.
     * @return An Optional containing the Watchlist if found.
     */
    @Query(value = "SELECT * FROM watchlist WHERE name = :name AND user_id = :userId", nativeQuery = true)
    Optional<Watchlist> findByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);

    /**
     * Finds a watchlist by name and user entity.
     *
     * @param name The watchlist name.
     * @param user The AppUser entity.
     * @return An Optional containing the Watchlist if found.
     */
    default Optional<Watchlist> findByNameAndUser(String name, AppUser user) {
        return findByNameAndUserId(name, user.getUserId());
    }

    /**
     * Checks if a watchlist with the given name exists for a user.
     *
     * @param name   The watchlist name.
     * @param userId The user's ID.
     * @return true if a watchlist with this name exists for the user.
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM watchlist WHERE name = :name AND user_id = :userId", nativeQuery = true)
    Integer existsByNameAndUserIdNative(@Param("name") String name, @Param("userId") Long userId);

    default boolean existsByNameAndUserId(String name, Long userId) {
        return existsByNameAndUserIdNative(name, userId) == 1;
    }

    /**
     * Checks if a watchlist with the given name exists for a user entity.
     *
     * @param name The watchlist name.
     * @param user The AppUser entity.
     * @return true if a watchlist with this name exists for the user.
     */
    default boolean existsByNameAndUser(String name, AppUser user) {
        return existsByNameAndUserId(name, user.getUserId());
    }

    /**
     * Deletes all watchlists for a user.
     *
     * @param userId The user's ID.
     * @return Number of rows deleted.
     */
    @Modifying
    @Query(value = "DELETE FROM watchlist WHERE user_id = :userId", nativeQuery = true)
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Deletes all watchlists for a user entity.
     *
     * @param user The AppUser entity.
     */
    default void deleteByUser(AppUser user) {
        deleteByUserId(user.getUserId());
    }
}