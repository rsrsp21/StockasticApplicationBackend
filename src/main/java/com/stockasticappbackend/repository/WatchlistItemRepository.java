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

import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.Watchlist;
import com.stockasticappbackend.model.entity.WatchlistItem;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

       /**
        * Finds all items in a watchlist ordered by added date (newest first).
        *
        * @param watchlistId The watchlist's ID.
        * @return A list of WatchlistItem entities.
        */
       @Query(value = "SELECT * FROM watchlist_item WHERE watchlist_id = :watchlistId ORDER BY added_at DESC", nativeQuery = true)
       List<WatchlistItem> findByWatchlistId(@Param("watchlistId") Long watchlistId);

       /**
        * Finds all items in a watchlist entity ordered by added date.
        *
        * @param watchlist The Watchlist entity.
        * @return A list of WatchlistItem entities.
        */
       default List<WatchlistItem> findByWatchlistOrderByAddedAtDesc(Watchlist watchlist) {
              return findByWatchlistId(watchlist.getWatchlistId());
       }

       /**
        * Finds all items in a watchlist (paginated, newest first).
        *
        * @param watchlistId The watchlist's ID.
        * @param pageable    Pagination parameters.
        * @return A page of WatchlistItem entities.
        */
       @Query(value = "SELECT * FROM watchlist_item WHERE watchlist_id = :watchlistId ORDER BY added_at DESC", countQuery = "SELECT COUNT(*) FROM watchlist_item WHERE watchlist_id = :watchlistId", nativeQuery = true)
       Page<WatchlistItem> findByWatchlistIdPaged(@Param("watchlistId") Long watchlistId,
                     Pageable pageable);

       /**
        * Finds all items in a watchlist entity (paginated).
        *
        * @param watchlist The Watchlist entity.
        * @param pageable  Pagination parameters.
        * @return A page of WatchlistItem entities.
        */
       default Page<WatchlistItem> findByWatchlistOrderByAddedAtDesc(Watchlist watchlist, Pageable pageable) {
              return findByWatchlistIdPaged(watchlist.getWatchlistId(), pageable);
       }

       /**
        * Finds a watchlist item by watchlist ID and stock ID.
        *
        * @param watchlistId The watchlist's ID.
        * @param stockId     The stock's ID.
        * @return An Optional containing the WatchlistItem if found.
        */
       @Query(value = "SELECT * FROM watchlist_item WHERE watchlist_id = :watchlistId AND stock_id = :stockId", nativeQuery = true)
       Optional<WatchlistItem> findByWatchlistIdAndStockId(@Param("watchlistId") Long watchlistId,
                     @Param("stockId") Long stockId);

       /**
        * Finds a watchlist item by watchlist and stock entities.
        *
        * @param watchlist The Watchlist entity.
        * @param stock     The Stock entity.
        * @return An Optional containing the WatchlistItem if found.
        */
       default Optional<WatchlistItem> findByWatchlistAndStock(Watchlist watchlist, Stock stock) {
              return findByWatchlistIdAndStockId(watchlist.getWatchlistId(), stock.getStockId());
       }

       /**
        * Checks if a stock exists in a watchlist.
        *
        * @param watchlistId The watchlist's ID.
        * @param stockId     The stock's ID.
        * @return true if the stock is in the watchlist.
        */
       @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM watchlist_item WHERE watchlist_id = :watchlistId AND stock_id = :stockId", nativeQuery = true)
       Integer existsByWatchlistIdAndStockIdNative(@Param("watchlistId") Long watchlistId,
                     @Param("stockId") Long stockId);

       default boolean existsByWatchlistIdAndStockId(Long watchlistId, Long stockId) {
              return existsByWatchlistIdAndStockIdNative(watchlistId, stockId) == 1;
       }

       /**
        * Checks if a stock exists in a watchlist (entity version).
        *
        * @param watchlist The Watchlist entity.
        * @param stock     The Stock entity.
        * @return true if the stock is in the watchlist.
        */
       default boolean existsByWatchlistAndStock(Watchlist watchlist, Stock stock) {
              return existsByWatchlistIdAndStockId(watchlist.getWatchlistId(), stock.getStockId());
       }

       /**
        * Counts items in a watchlist.
        *
        * @param watchlistId The watchlist's ID.
        * @return The number of items.
        */
       @Query(value = "SELECT COUNT(*) FROM watchlist_item WHERE watchlist_id = :watchlistId", nativeQuery = true)
       long countByWatchlistId(@Param("watchlistId") Long watchlistId);

       /**
        * Counts items in a watchlist entity.
        *
        * @param watchlist The Watchlist entity.
        * @return The number of items.
        */
       default long countByWatchlist(Watchlist watchlist) {
              return countByWatchlistId(watchlist.getWatchlistId());
       }

       /**
        * Finds all watchlist items for a stock across all user's watchlists.
        *
        * @param stockId The stock's ID.
        * @param userId  The user's ID.
        * @return A list of WatchlistItem entities.
        */
       @Query(value = "SELECT wi.* FROM watchlist_item wi " +
                     "JOIN watchlist w ON wi.watchlist_id = w.watchlist_id " +
                     "WHERE wi.stock_id = :stockId AND w.user_id = :userId", nativeQuery = true)
       List<WatchlistItem> findByStockIdAndUserId(@Param("stockId") Long stockId,
                     @Param("userId") Long userId);

       /**
        * Deletes all items from a watchlist.
        *
        * @param watchlistId The watchlist's ID.
        * @return Number of rows deleted.
        */
       @Modifying
       @Query(value = "DELETE FROM watchlist_item WHERE watchlist_id = :watchlistId", nativeQuery = true)
       int deleteByWatchlistId(@Param("watchlistId") Long watchlistId);

       /**
        * Deletes all items from a watchlist entity.
        *
        * @param watchlist The Watchlist entity.
        */
       default void deleteByWatchlist(Watchlist watchlist) {
              deleteByWatchlistId(watchlist.getWatchlistId());
       }

       /**
        * Deletes all watchlist items containing a specific stock.
        *
        * @param stockId The stock's ID.
        * @return Number of rows deleted.
        */
       @Modifying
       @Query(value = "DELETE FROM watchlist_item WHERE stock_id = :stockId", nativeQuery = true)
       int deleteByStockId(@Param("stockId") Long stockId);

       /**
        * Deletes all watchlist items containing a specific stock entity.
        *
        * @param stock The Stock entity.
        */
       default void deleteByStock(Stock stock) {
              deleteByStockId(stock.getStockId());
       }

       /**
        * Finds a watchlist item by ID and user ID.
        *
        * @param itemId The watchlist item's ID.
        * @param userId The user's ID.
        * @return An Optional containing the WatchlistItem if found.
        */
       @Query(value = "SELECT wi.* FROM watchlist_item wi " +
                     "JOIN watchlist w ON wi.watchlist_id = w.watchlist_id " +
                     "WHERE wi.watchlist_item_id = :itemId AND w.user_id = :userId", nativeQuery = true)
       Optional<WatchlistItem> findByIdAndUserId(@Param("itemId") Long itemId,
                     @Param("userId") Long userId);
}