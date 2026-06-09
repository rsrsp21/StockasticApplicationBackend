package com.stockasticappbackend.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;

/**
 * Repository interface for StockPrice entity operations.
 * Provides CRUD operations and custom query methods for stock price data.
 * Includes methods for fetching latest prices, historical data, and cleanup.
 */
@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

        /**
         * Finds the latest price for a stock by stock ID.
         *
         * @param stockId The stock's ID.
         * @return An Optional containing the latest StockPrice.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId ORDER BY price_time DESC LIMIT 1", nativeQuery = true)
        Optional<StockPrice> findLatestByStockId(@Param("stockId") Long stockId);

        /**
         * Finds the latest price for a stock entity.
         *
         * @param stock The Stock entity.
         * @return An Optional containing the latest StockPrice.
         */
        default Optional<StockPrice> findTopByStockOrderByPriceTimeDesc(Stock stock) {
                return findLatestByStockId(stock.getStockId());
        }

        /**
         * Finds the oldest price for a stock by stock ID.
         *
         * @param stockId The stock's ID.
         * @return An Optional containing the oldest StockPrice.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId ORDER BY price_time ASC LIMIT 1", nativeQuery = true)
        Optional<StockPrice> findOldestByStockId(@Param("stockId") Long stockId);

        /**
         * Finds the oldest price for a stock entity.
         *
         * @param stock The Stock entity.
         * @return An Optional containing the oldest StockPrice.
         */
        default Optional<StockPrice> findTopByStockOrderByPriceTimeAsc(Stock stock) {
                return findOldestByStockId(stock.getStockId());
        }

        /**
         * Finds the latest price for a stock by symbol.
         *
         * @param symbol The stock's symbol.
         * @return An Optional containing the latest StockPrice.
         */
        @Query(value = "SELECT sp.* FROM stock_price sp " +
                        "JOIN stock s ON sp.stock_id = s.stock_id " +
                        "WHERE s.symbol = :symbol ORDER BY sp.price_time DESC LIMIT 1", nativeQuery = true)
        Optional<StockPrice> findLatestBySymbol(@Param("symbol") String symbol);

        /**
         * Finds price history for a stock within a time range.
         *
         * @param stockId   The stock's ID.
         * @param startTime The start of the range.
         * @param endTime   The end of the range.
         * @return A list of StockPrice entities ordered by time ascending.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId " +
                        "AND price_time BETWEEN :startTime AND :endTime ORDER BY price_time ASC", nativeQuery = true)
        List<StockPrice> findPriceHistoryByStockId(
                        @Param("stockId") Long stockId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * Finds price history for a stock entity within a time range.
         *
         * @param stock     The Stock entity.
         * @param startTime The start of the range.
         * @param endTime   The end of the range.
         * @return A list of StockPrice entities ordered by time ascending.
         */
        default List<StockPrice> findByStockAndPriceTimeBetweenOrderByPriceTimeAsc(
                        Stock stock, LocalDateTime startTime, LocalDateTime endTime) {
                return findPriceHistoryByStockId(stock.getStockId(), startTime, endTime);
        }

        /**
         * Finds all prices for a stock by ID (paginated, newest first).
         *
         * @param stockId  The stock's ID.
         * @param pageable Pagination parameters.
         * @return A page of StockPrice entities.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId ORDER BY price_time DESC", countQuery = "SELECT COUNT(*) FROM stock_price WHERE stock_id = :stockId", nativeQuery = true)
        Page<StockPrice> findByStockIdOrderByPriceTimeDesc(@Param("stockId") Long stockId, Pageable pageable);

        /**
         * Finds all prices for a stock entity (paginated, newest first).
         *
         * @param stock    The Stock entity.
         * @param pageable Pagination parameters.
         * @return A page of StockPrice entities.
         */
        default Page<StockPrice> findByStockOrderByPriceTimeDesc(Stock stock, Pageable pageable) {
                return findByStockIdOrderByPriceTimeDesc(stock.getStockId(), pageable);
        }

        /**
         * Finds the latest prices for all active stocks.
         *
         * @return A list of the latest StockPrice for each active stock.
         */
        @Query(value = "SELECT sp.* FROM stock_price sp " +
                        "JOIN stock s ON sp.stock_id = s.stock_id " +
                        "WHERE s.is_active = true " +
                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = sp.stock_id)", nativeQuery = true)
        List<StockPrice> findLatestPricesForActiveStocks();

        /**
         * Deletes all prices older than the specified cutoff time.
         *
         * @param cutoffTime The cutoff timestamp.
         * @return Number of rows deleted.
         */
        @Modifying
        @Transactional
        @Query(value = "DELETE FROM stock_price WHERE price_time < :cutoffTime", nativeQuery = true)
        int deleteByPriceTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

        /**
         * Counts prices for a stock on a specific date.
         *
         * @param stockId The stock's ID.
         * @param date    The date to count.
         * @return The count of price records.
         */
        @Query(value = "SELECT COUNT(*) FROM stock_price WHERE stock_id = :stockId " +
                        "AND DATE(price_time) = :date", nativeQuery = true)
        long countByStockIdAndDate(@Param("stockId") Long stockId, @Param("date") LocalDate date);

        /**
         * Deletes all prices for a stock by ID.
         *
         * @param stockId The stock's ID.
         * @return Number of rows deleted.
         */
        @Modifying
        @Transactional
        @Query(value = "DELETE FROM stock_price WHERE stock_id = :stockId", nativeQuery = true)
        int deleteByStockId(@Param("stockId") Long stockId);

        /**
         * Deletes all prices for a stock entity.
         *
         * @param stock The Stock entity.
         */
        default void deleteByStock(Stock stock) {
                deleteByStockId(stock.getStockId());
        }

        /**
         * Finds all prices for a stock on a specific date.
         *
         * @param stockId The stock's ID.
         * @param date    The date to search.
         * @return A list of StockPrice entities ordered by time ascending.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId " +
                        "AND DATE(price_time) = :date ORDER BY price_time ASC", nativeQuery = true)
        List<StockPrice> findByStockIdAndDate(@Param("stockId") Long stockId, @Param("date") LocalDate date);

        /**
         * Checks if a price record exists for a stock at a specific timestamp.
         * Used to prevent duplicate entries during server restarts.
         *
         * @param stockId   The stock's ID.
         * @param priceTime The exact timestamp to check.
         * @return 1 if a record exists, 0 if not (can be used as boolean in Java)
         */
        @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM stock_price WHERE stock_id = :stockId " +
                        "AND price_time = :priceTime", nativeQuery = true)
        Integer existsByStockIdAndPriceTime(@Param("stockId") Long stockId,
                        @Param("priceTime") LocalDateTime priceTime);

        /**
         * Checks if a price record exists for a stock entity at a specific timestamp.
         *
         * @param stock     The Stock entity.
         * @param priceTime The exact timestamp to check.
         * @return true if a record exists, false otherwise.
         */
        default boolean existsByStockAndPriceTime(Stock stock, LocalDateTime priceTime) {
                return existsByStockIdAndPriceTime(stock.getStockId(), priceTime) == 1;
        }

        /**
         * Deletes all prices for a stock except the one with the specified ID.
         * Optimized for resetting the day's data without fetching entities into memory.
         *
         * @param stockId  The stock's ID.
         * @param anchorId The ID of the StockPrice to keep.
         * @return Number of rows deleted.
         */
        @Modifying
        @Transactional
        @Query(value = "DELETE FROM stock_price WHERE stock_id = :stockId AND price_id != :anchorId", nativeQuery = true)
        int deleteByStockIdAndPriceIdNot(@Param("stockId") Long stockId, @Param("anchorId") Long anchorId);

        /**
         * Deletes all prices for a stock entity except the one with the specified ID.
         *
         * @param stock    The Stock entity.
         * @param anchorId The ID of the StockPrice to keep.
         * @return Number of rows deleted.
         */
        default int deleteByStockAndPriceIdNot(Stock stock, Long anchorId) {
                return deleteByStockIdAndPriceIdNot(stock.getStockId(), anchorId);
        }

        /**
         * Checks if a price record exists for a stock at a specific minute (ignoring
         * seconds).
         * Used to catch "dirty" timestamps like 14:00:23 when trying to insert clean
         * 14:00:00.
         * Prevents visual duplicates on charts.
         *
         * @param stockId The stock's ID.
         * @param date    The date to check.
         * @param hour    The hour to check (0-23).
         * @param minute  The minute to check (0-59).
         * @return true if a record exists for that minute, false otherwise.
         */
        @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM stock_price WHERE stock_id = :stockId " +
                        "AND DATE(price_time) = :date " +
                        "AND EXTRACT(HOUR FROM price_time) = :hour " +
                        "AND EXTRACT(MINUTE FROM price_time) = :minute", nativeQuery = true)
        Integer existsByStockIdAndMinuteNative(
                        @Param("stockId") Long stockId,
                        @Param("date") LocalDate date,
                        @Param("hour") int hour,
                        @Param("minute") int minute);

        default boolean existsByStockIdAndMinute(Long stockId, LocalDate date, int hour, int minute) {
                return existsByStockIdAndMinuteNative(stockId, date, hour, minute) == 1;
        }

        /**
         * Finds prices in a specific time window (e.g. 12:00:00 to 12:00:01).
         * Used for deduping.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId " +
                        "AND price_time >= :startTime AND price_time < :endTime", nativeQuery = true)
        List<StockPrice> findByStockIdAndPriceTimeBetween(
                        @Param("stockId") Long stockId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        /**
         * Finds the most recent candles before a given timestamp.
         * Ordered by price_time DESC (latest first).
         *
         * @param stockId    The stock's ID.
         * @param beforeTime Exclusive upper-bound timestamp.
         * @param limit      Maximum number of rows to return.
         * @return List of StockPrice entities in descending order.
         */
        @Query(value = "SELECT * FROM stock_price WHERE stock_id = :stockId " +
                        "AND price_time < :beforeTime ORDER BY price_time DESC LIMIT :limit", nativeQuery = true)
        List<StockPrice> findRecentPricesBeforeTime(
                        @Param("stockId") Long stockId,
                        @Param("beforeTime") LocalDateTime beforeTime,
                        @Param("limit") int limit);

        /**
         * Deletes duplicate stock price records, keeping only the most recent entry (highest ID)
         * for each stock_id and price_time combination.
         *
         * @return Number of rows deleted.
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query(value = "DELETE FROM stock_price sp1 " +
                        "USING stock_price sp2 " +
                        "WHERE sp1.stock_id = sp2.stock_id " +
                        "AND sp1.price_time = sp2.price_time " +
                        "AND sp1.price_id < sp2.price_id", nativeQuery = true)
        int deleteDuplicateRecords();

        /**
         * Deletes all "dirty" price records where SECOND is not 0.
         * These are in-progress candles that Yahoo returned with timestamps like
         * 10:36:37.
         * Should be called during cleanup to remove legacy dirty data.
         *
         * @return Number of rows deleted.
         */
        @Modifying
        @Transactional
        @Query(value = "DELETE FROM stock_price WHERE EXTRACT(SECOND FROM price_time) <> 0", nativeQuery = true)
        int deleteDirtyTimestamps();
    /**
     * Checks if existence of prices for a stock on a specific date.
     *
     * @param stockId The stock's ID.
     * @param date    The date to count.
     * @return true if price records exist.
     */
    default boolean existsByStockIdAndDate(Long stockId, LocalDate date) {
        return countByStockIdAndDate(stockId, date) > 0;
    }

        /**
         * Calculates total volume for a stock on a specific date.
         *
         * @param stockId The stock's ID.
         * @param date    The date to sum.
         * @return The total volume (sum of all candle volumes).
         */
        @Query(value = "SELECT COALESCE(SUM(interval_volume), 0) FROM stock_price WHERE stock_id = :stockId AND DATE(price_time) = :date", nativeQuery = true)
        Long sumVolumeByStockIdAndDate(@Param("stockId") Long stockId, @Param("date") LocalDate date);

        /**
         * Calculates average volume (non-zero) for a stock on a specific date.
         *
         * @param stockId The stock's ID.
         * @param date    The date to avg.
         * @return The average volume per candle.
         */
        @Query(value = "SELECT AVG(interval_volume) FROM stock_price WHERE stock_id = :stockId AND DATE(price_time) = :date AND interval_volume > 0", nativeQuery = true)
        Double getAvgVolumeByStockIdAndDate(@Param("stockId") Long stockId, @Param("date") LocalDate date);

}
