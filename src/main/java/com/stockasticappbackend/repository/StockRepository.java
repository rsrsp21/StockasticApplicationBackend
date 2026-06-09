package com.stockasticappbackend.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.Stock;

/**
 * Repository interface for Stock entity operations.
 * Provides CRUD operations and custom query methods for stock data.
 * Includes methods for searching, filtering, and pagination.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

        /**
         * Finds a stock by its symbol.
         *
         * @param symbol The stock symbol.
         * @return An Optional containing the Stock if found.
         */
        @Query(value = "SELECT * FROM stock WHERE symbol = :symbol", nativeQuery = true)
        Optional<Stock> findBySymbol(@Param("symbol") String symbol);

        /**
         * Checks if a stock exists with the given symbol.
         *
         * @param symbol The stock symbol.
         * @return true if a stock exists, false otherwise.
         */
        @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM stock WHERE symbol = :symbol", nativeQuery = true)
        Integer existsBySymbolNative(@Param("symbol") String symbol);

        default boolean existsBySymbol(String symbol) {
                return existsBySymbolNative(symbol) == 1;
        }

        /**
         * Finds all active stocks.
         *
         * @return A list of active Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE is_active = true", nativeQuery = true)
        List<Stock> findByIsActiveTrue();

        /**
         * Finds all active stocks (paginated).
         *
         * @param pageable Pagination parameters.
         * @return A page of active Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE is_active = true", countQuery = "SELECT COUNT(*) FROM stock WHERE is_active = true", nativeQuery = true)
        Page<Stock> findByIsActiveTruePaged(Pageable pageable);

        /**
         * Finds all active stocks (paginated) - overload for compatibility.
         *
         * @param pageable Pagination parameters.
         * @return A page of active Stock entities.
         */
        default Page<Stock> findByIsActiuveTrue(Pageable pageable) {
                return findByIsActiveTruePaged(pageable);
        }

        /**
         * Finds stocks by exchange.
         *
         * @param exchange The exchange name.
         * @return A list of Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE exchange = :exchange", nativeQuery = true)
        List<Stock> findByExchange(@Param("exchange") String exchange);

        /**
         * Finds stocks by exchange (paginated).
         *
         * @param exchange The exchange name.
         * @param pageable Pagination parameters.
         * @return A page of Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE exchange = :exchange", countQuery = "SELECT COUNT(*) FROM stock WHERE exchange = :exchange", nativeQuery = true)
        Page<Stock> findByExchangePaged(@Param("exchange") String exchange, Pageable pageable);

        /**
         * Finds stocks by sector.
         *
         * @param sector The sector name.
         * @return A list of Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE sector = :sector", nativeQuery = true)
        List<Stock> findBySector(@Param("sector") String sector);

        /**
         * Finds stocks by sector (paginated).
         *
         * @param sector   The sector name.
         * @param pageable Pagination parameters.
         * @return A page of Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE sector = :sector", countQuery = "SELECT COUNT(*) FROM stock WHERE sector = :sector", nativeQuery = true)
        Page<Stock> findBySectorPaged(@Param("sector") String sector, Pageable pageable);

        /**
         * Finds stocks by exchange and active status.
         *
         * @param exchange The exchange name.
         * @param isActive The active status.
         * @return A list of Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE exchange = :exchange AND is_active = :isActive", nativeQuery = true)
        List<Stock> findByExchangeAndIsActive(@Param("exchange") String exchange, @Param("isActive") Boolean isActive);

        /**
         * Finds stocks by exchange and active status (paginated).
         *
         * @param exchange The exchange name.
         * @param isActive The active status.
         * @param pageable Pagination parameters.
         * @return A page of Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE exchange = :exchange AND is_active = :isActive", countQuery = "SELECT COUNT(*) FROM stock WHERE exchange = :exchange AND is_active = :isActive", nativeQuery = true)
        Page<Stock> findByExchangeAndIsActivePaged(@Param("exchange") String exchange,
                        @Param("isActive") Boolean isActive,
                        Pageable pageable);

        /**
         * Searches stocks by name or symbol (case insensitive).
         *
         * @param name   The name search term.
         * @param symbol The symbol search term.
         * @return A list of matching Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
                        "OR LOWER(symbol) LIKE LOWER(CONCAT('%', :symbol, '%'))", nativeQuery = true)
        List<Stock> findByNameOrSymbolContaining(@Param("name") String name, @Param("symbol") String symbol);

        /**
         * Searches stocks by name or symbol (case insensitive, paginated).
         *
         * @param name     The name search term.
         * @param symbol   The symbol search term.
         * @param pageable Pagination parameters.
         * @return A page of matching Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
                        "OR LOWER(symbol) LIKE LOWER(CONCAT('%', :symbol, '%'))", countQuery = "SELECT COUNT(*) FROM stock WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) "
                                        +
                                        "OR LOWER(symbol) LIKE LOWER(CONCAT('%', :symbol, '%'))", nativeQuery = true)
        Page<Stock> findByNameOrSymbolContainingPaged(@Param("name") String name,
                        @Param("symbol") String symbol,
                        Pageable pageable);

        /**
         * Finds stocks by symbol containing (case insensitive, paginated).
         *
         * @param symbol   The symbol search term.
         * @param pageable Pagination parameters.
         * @return A page of matching Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE LOWER(symbol) LIKE LOWER(CONCAT('%', :symbol, '%'))", countQuery = "SELECT COUNT(*) FROM stock WHERE LOWER(symbol) LIKE LOWER(CONCAT('%', :symbol, '%'))", nativeQuery = true)
        Page<Stock> findBySymbolContainingPaged(@Param("symbol") String symbol, Pageable pageable);

        /**
         * Searches active stocks by query term.
         *
         * @param query    The search term for name or symbol.
         * @param pageable Pagination parameters.
         * @return A page of matching active Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE is_active = true AND " +
                        "(LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(symbol) LIKE LOWER(CONCAT('%', :query, '%')))", countQuery = "SELECT COUNT(*) FROM stock WHERE is_active = true AND "
                                        +
                                        "(LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                                        "LOWER(symbol) LIKE LOWER(CONCAT('%', :query, '%')))", nativeQuery = true)
        Page<Stock> searchActiveStocks(@Param("query") String query, Pageable pageable);

        /**
         * Searches all stocks by query term (for admin).
         *
         * @param query    The search term for name or symbol.
         * @param pageable Pagination parameters.
         * @return A page of matching Stock entities.
         */
        @Query(value = "SELECT * FROM stock WHERE " +
                        "LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(symbol) LIKE LOWER(CONCAT('%', :query, '%'))", countQuery = "SELECT COUNT(*) FROM stock WHERE "
                                        +
                                        "LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                                        "LOWER(symbol) LIKE LOWER(CONCAT('%', :query, '%'))", nativeQuery = true)
        Page<Stock> searchAllStocks(@Param("query") String query, Pageable pageable);

        /**
         * Searches active stocks with optional filters.
         *
         * @param query     The search term (optional).
         * @param sector    The sector filter (optional).
         * @param exchange  The exchange filter (optional).
         * @param minPrice  Minimum price filter (optional).
         * @param maxPrice  Maximum price filter (optional).
         * @param minVolume Minimum volume filter (optional).
         * @param maxVolume Maximum volume filter (optional).
         * @param pageable  Pagination parameters.
         * @return A page of matching active Stock entities.
         */
        @Query(value = "SELECT s.* FROM stock s WHERE s.is_active = true " +
                        "AND (:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%'))) "
                        +
                        "AND (:sector IS NULL OR :sector = '' OR LOWER(s.sector) = LOWER(:sector)) " +
                   "AND (:exchange IS NULL OR :exchange = '' OR LOWER(s.exchange) = LOWER(:exchange)) " +
                   "AND (:marketType IS NULL OR :marketType = '' OR " +
                   "(LOWER(:marketType) = 'domestic' AND UPPER(s.exchange) IN ('NSE', 'BSE')) OR " +
                   "(LOWER(:marketType) = 'international' AND UPPER(s.exchange) NOT IN ('NSE', 'BSE'))) " +
                   "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR " +
                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id) "
                        +
                        "AND (:minPrice IS NULL OR sp.interval_close >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR sp.interval_close <= :maxPrice))) " +
                        "AND ((:minVolume IS NULL AND :maxVolume IS NULL) OR " +
                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id AND sp2.interval_volume > 0) "
                        +
                        "AND (:minVolume IS NULL OR sp.interval_volume >= :minVolume) " +
                        "AND (:maxVolume IS NULL OR sp.interval_volume <= :maxVolume)))", countQuery = "SELECT COUNT(*) FROM stock s WHERE s.is_active = true "
                                        +
                                        "AND (:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%'))) "
                                        +
                                        "AND (:sector IS NULL OR :sector = '' OR LOWER(s.sector) = LOWER(:sector)) " +
                                   "AND (:exchange IS NULL OR :exchange = '' OR LOWER(s.exchange) = LOWER(:exchange)) "
                                   +
                                   "AND (:marketType IS NULL OR :marketType = '' OR " +
                                   "(LOWER(:marketType) = 'domestic' AND UPPER(s.exchange) IN ('NSE', 'BSE')) OR " +
                                   "(LOWER(:marketType) = 'international' AND UPPER(s.exchange) NOT IN ('NSE', 'BSE'))) "
                                   +
                                        "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR " +
                                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id) "
                                        +
                                        "AND (:minPrice IS NULL OR sp.interval_close >= :minPrice) " +
                                        "AND (:maxPrice IS NULL OR sp.interval_close <= :maxPrice))) " +
                                        "AND ((:minVolume IS NULL AND :maxVolume IS NULL) OR " +
                                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id AND sp2.interval_volume > 0) "
                                        +
                                        "AND (:minVolume IS NULL OR sp.interval_volume >= :minVolume) " +
                                        "AND (:maxVolume IS NULL OR sp.interval_volume <= :maxVolume)))", nativeQuery = true)
        Page<Stock> searchStocksWithFilters(
                        @Param("query") String query,
                        @Param("sector") String sector,
                        @Param("exchange") String exchange,
                        @Param("marketType") String marketType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("minVolume") Long minVolume,
                        @Param("maxVolume") Long maxVolume,
                        Pageable pageable);

        /**
         * Searches all stocks with optional filters (for admin).
         *
         * @param query     The search term (optional).
         * @param sector    The sector filter (optional).
         * @param exchange  The exchange filter (optional).
         * @param minPrice  Minimum price filter (optional).
         * @param maxPrice  Maximum price filter (optional).
         * @param minVolume Minimum volume filter (optional).
         * @param maxVolume Maximum volume filter (optional).
         * @param pageable  Pagination parameters.
         * @return A page of matching Stock entities.
         */
        @Query(value = "SELECT s.* FROM stock s WHERE " +
                        "(:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%'))) "
                        +
                        "AND (:sector IS NULL OR :sector = '' OR LOWER(s.sector) = LOWER(:sector)) " +
                   "AND (:exchange IS NULL OR :exchange = '' OR LOWER(s.exchange) = LOWER(:exchange)) " +
                   "AND (:marketType IS NULL OR :marketType = '' OR " +
                   "(LOWER(:marketType) = 'domestic' AND UPPER(s.exchange) IN ('NSE', 'BSE')) OR " +
                   "(LOWER(:marketType) = 'international' AND UPPER(s.exchange) NOT IN ('NSE', 'BSE'))) " +
                   "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR " +
                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id) "
                        +
                        "AND (:minPrice IS NULL OR sp.interval_close >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR sp.interval_close <= :maxPrice))) " +
                        "AND ((:minVolume IS NULL AND :maxVolume IS NULL) OR " +
                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id AND sp2.interval_volume > 0) "
                        +
                        "AND (:minVolume IS NULL OR sp.interval_volume >= :minVolume) " +
                        "AND (:maxVolume IS NULL OR sp.interval_volume <= :maxVolume)))", countQuery = "SELECT COUNT(*) FROM stock s WHERE "
                                        +
                                        "(:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%'))) "
                                        +
                                        "AND (:sector IS NULL OR :sector = '' OR LOWER(s.sector) = LOWER(:sector)) " +
                                   "AND (:exchange IS NULL OR :exchange = '' OR LOWER(s.exchange) = LOWER(:exchange)) "
                                   +
                                   "AND (:marketType IS NULL OR :marketType = '' OR " +
                                   "(LOWER(:marketType) = 'domestic' AND UPPER(s.exchange) IN ('NSE', 'BSE')) OR " +
                                   "(LOWER(:marketType) = 'international' AND UPPER(s.exchange) NOT IN ('NSE', 'BSE'))) "
                                   +
                                        "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR " +
                                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id) "
                                        +
                                        "AND (:minPrice IS NULL OR sp.interval_close >= :minPrice) " +
                                        "AND (:maxPrice IS NULL OR sp.interval_close <= :maxPrice))) " +
                                        "AND ((:minVolume IS NULL AND :maxVolume IS NULL) OR " +
                                        "EXISTS (SELECT 1 FROM stock_price sp WHERE sp.stock_id = s.stock_id " +
                                        "AND sp.price_time = (SELECT MAX(sp2.price_time) FROM stock_price sp2 WHERE sp2.stock_id = s.stock_id AND sp2.interval_volume > 0) "
                                        +
                                        "AND (:minVolume IS NULL OR sp.interval_volume >= :minVolume) " +
                                        "AND (:maxVolume IS NULL OR sp.interval_volume <= :maxVolume)))", nativeQuery = true)
        Page<Stock> searchAllStocksWithFilters(
                        @Param("query") String query,
                        @Param("sector") String sector,
                        @Param("exchange") String exchange,
                        @Param("marketType") String marketType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("minVolume") Long minVolume,
                        @Param("maxVolume") Long maxVolume,
                        Pageable pageable);
}
