package com.stockasticappbackend.service.marketai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stockasticappbackend.dto.marketai.CandleLookupResult;
import com.stockasticappbackend.dto.marketai.StockComparisonResult;

import lombok.RequiredArgsConstructor;

/**
 * Deterministic market-query executor. Blocking port of the reactive R2DBC
 * version: it issues the same SQL against the shared {@code stock}/{@code stock_price}
 * tables using {@link NamedParameterJdbcTemplate}.
 */
@Service
@RequiredArgsConstructor
public class MarketAiQueryExecutor {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CandleLookupResult findPriceAtTime(String symbol, String time) {
        LocalTime requestedTime = parseTime(time);
        StockRow stock = findStock(symbol);
        LocalDate tradingDate = latestTradingDate(stock.stockId());
        PriceRow candle = findCandle(stock.stockId(), tradingDate, requestedTime);

        return CandleLookupResult.builder()
                .stockId(stock.stockId())
                .symbol(stock.symbol())
                .stockName(stock.name())
                .tradingDate(tradingDate)
                .requestedTime(requestedTime)
                .matchedCandleTime(candle.priceTime())
                .price(candle.intervalClose())
                .volume(candle.intervalVolume())
                .build();
    }

    public String comparePrices(String symbol, String startTime, String endTime) {
        LocalTime first = parseTime(startTime);
        LocalTime second = parseTime(endTime);

        StockRow stock = findStock(symbol);
        LocalDate tradingDate = latestTradingDate(stock.stockId());
        PriceRow firstCandle = findCandle(stock.stockId(), tradingDate, first);
        PriceRow secondCandle = findCandle(stock.stockId(), tradingDate, second);

        BigDecimal firstPrice = firstCandle.intervalClose();
        BigDecimal secondPrice = secondCandle.intervalClose();
        BigDecimal change = secondPrice.subtract(firstPrice);
        BigDecimal percentChange = firstPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : change.divide(firstPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return """
                For %s on %s:
                - %s candle price: %s at %s
                - %s candle price: %s at %s
                - Absolute change: %s
                - Percentage change: %s%%
                """.formatted(
                stock.symbol(),
                tradingDate,
                first,
                firstPrice,
                firstCandle.priceTime().toLocalTime(),
                second,
                secondPrice,
                secondCandle.priceTime().toLocalTime(),
                change.setScale(2, RoundingMode.HALF_UP),
                percentChange.setScale(2, RoundingMode.HALF_UP));
    }

    public StockComparisonResult compareStocks(String primarySymbol, String comparisonSymbol) {
        PriceWithStock primary = findLatestPrice(primarySymbol);
        PriceWithStock comparison = findLatestPrice(comparisonSymbol);

        BigDecimal difference = primary.price().subtract(comparison.price());
        BigDecimal percentageDifference = comparison.price().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : difference.divide(comparison.price(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return StockComparisonResult.builder()
                .primarySymbol(primary.symbol())
                .comparisonSymbol(comparison.symbol())
                .primaryPrice(primary.price())
                .comparisonPrice(comparison.price())
                .primaryVolume(primary.volume())
                .comparisonVolume(comparison.volume())
                .priceDifference(difference.setScale(2, RoundingMode.HALF_UP))
                .percentageDifference(percentageDifference.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    public StockComparisonResult compareStocksAtTimes(
            String primarySymbol,
            String primaryTime,
            String comparisonSymbol,
            String comparisonSymbolTime) {
        LocalTime firstRequestedTime = parseTime(primaryTime);
        LocalTime secondRequestedTime = parseTime(comparisonSymbolTime);

        StockRow primaryStock = findStock(primarySymbol);
        StockRow comparisonStock = findStock(comparisonSymbol);

        LocalDate primaryDate = latestTradingDate(primaryStock.stockId());
        LocalDate comparisonDate = latestTradingDate(comparisonStock.stockId());
        LocalDate tradingDate = primaryDate.isBefore(comparisonDate) ? primaryDate : comparisonDate;

        PriceRow primaryCandle = findCandle(primaryStock.stockId(), tradingDate, firstRequestedTime);
        PriceRow comparisonCandle = findCandle(comparisonStock.stockId(), tradingDate, secondRequestedTime);

        BigDecimal difference = primaryCandle.intervalClose().subtract(comparisonCandle.intervalClose());
        BigDecimal percentageDifference = comparisonCandle.intervalClose().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : difference.divide(comparisonCandle.intervalClose(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return StockComparisonResult.builder()
                .primarySymbol(primaryStock.symbol())
                .comparisonSymbol(comparisonStock.symbol())
                .primaryPrice(primaryCandle.intervalClose())
                .comparisonPrice(comparisonCandle.intervalClose())
                .tradingDate(tradingDate)
                .primaryRequestedTime(firstRequestedTime)
                .primaryMatchedTime(primaryCandle.priceTime().toLocalTime())
                .comparisonRequestedTime(secondRequestedTime)
                .comparisonMatchedTime(comparisonCandle.priceTime().toLocalTime())
                .priceDifference(difference.setScale(2, RoundingMode.HALF_UP))
                .percentageDifference(percentageDifference.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    public String compareMultipleStocks(List<String> symbols) {
        if (symbols == null || symbols.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least two stock symbols are required for multi-stock comparison.");
        }

        List<PriceWithStock> results = symbols.stream()
                .distinct()
                .limit(5)
                .map(this::findLatestPrice)
                .sorted((left, right) -> right.price().compareTo(left.price()))
                .toList();

        StringBuilder answer = new StringBuilder("Latest price comparison:\n");
        for (PriceWithStock result : results) {
            answer.append("- ").append(result.symbol()).append(": ").append(result.price()).append('\n');
        }
        return answer.toString().trim();
    }

    public String findHighestAfterTime(String symbol, String time) {
        LocalTime requestedTime = parseTime(time);
        StockRow stock = findStock(symbol);
        LocalDate tradingDate = latestTradingDate(stock.stockId());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("stockId", stock.stockId())
                .addValue("tradingDate", java.sql.Date.valueOf(tradingDate))
                .addValue("startTime", Timestamp.valueOf(tradingDate.atTime(requestedTime)));

        List<PriceRow> rows = jdbcTemplate.query("""
                SELECT price_time, interval_close, interval_volume
                FROM stock_price
                WHERE stock_id = :stockId
                  AND DATE(price_time) = :tradingDate
                  AND price_time >= :startTime
                ORDER BY interval_close DESC, price_time ASC
                LIMIT 1
                """, params, PRICE_ROW_MAPPER);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No candle found after %s for %s.".formatted(requestedTime, stock.symbol()));
        }

        PriceRow candle = rows.get(0);
        return """
                Highest price for %s after %s on %s:
                - Price: %s
                - Matched candle: %s
                - Volume: %s
                """.formatted(
                stock.symbol(),
                requestedTime,
                tradingDate,
                candle.intervalClose(),
                candle.priceTime().toLocalTime(),
                candle.intervalVolume() == null ? "N/A" : candle.intervalVolume());
    }

    public String topMoversInSector(String sector, String direction) {
        boolean losers = "losers".equalsIgnoreCase(direction);
        String normalizedSector = sector == null || sector.isBlank() ? null : sector.trim();
        String sectorClause = normalizedSector == null ? "" : "WHERE LOWER(s.sector) = LOWER(:sector)";

        String sql = """
                SELECT s.symbol,
                       latest.interval_close AS latest_price,
                       first_candle.interval_close AS open_price,
                       ROUND(((latest.interval_close - first_candle.interval_close) / NULLIF(first_candle.interval_close, 0)) * 100, 2) AS pct_change
                FROM stock s
                JOIN LATERAL (
                    SELECT CAST(MAX(price_time) AS DATE) AS trading_date
                    FROM stock_price
                    WHERE stock_id = s.stock_id
                ) trade_date ON TRUE
                JOIN LATERAL (
                    SELECT interval_close
                    FROM stock_price
                    WHERE stock_id = s.stock_id
                      AND DATE(price_time) = trade_date.trading_date
                    ORDER BY price_time ASC
                    LIMIT 1
                ) first_candle ON TRUE
                JOIN LATERAL (
                    SELECT interval_close, price_time
                    FROM stock_price
                    WHERE stock_id = s.stock_id
                      AND DATE(price_time) = trade_date.trading_date
                    ORDER BY price_time DESC
                    LIMIT 1
                ) latest ON TRUE
                """ + sectorClause + """
                ORDER BY pct_change """ + (losers ? "ASC" : "DESC") + """
                LIMIT 5
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (normalizedSector != null) {
            params.addValue("sector", normalizedSector);
        }

        List<SectorMoverRow> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> new SectorMoverRow(
                rs.getString("symbol"),
                rs.getBigDecimal("latest_price"),
                rs.getBigDecimal("open_price"),
                rs.getBigDecimal("pct_change")));

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    normalizedSector == null ? "No mover data found." : "No stocks found for sector " + normalizedSector + ".");
        }

        String label = losers ? "Top losers" : "Top movers";
        if ("gainers".equalsIgnoreCase(direction)) {
            label = "Top gainers";
        }

        StringBuilder answer = new StringBuilder(
                normalizedSector == null ? label + ":\n" : label + " in " + normalizedSector + " sector:\n");
        for (SectorMoverRow row : rows) {
            answer.append("- ")
                    .append(row.symbol())
                    .append(": latest ")
                    .append(row.latestPrice())
                    .append(", change ")
                    .append(row.pctChange())
                    .append("%\n");
        }
        return answer.toString().trim();
    }

    public String volumeQuery(String symbol, String startTime, String endTime, String sector) {
        if (symbol != null && startTime != null && endTime != null) {
            LocalTime start = parseTime(startTime);
            LocalTime end = parseTime(endTime);

            StockRow stock = findStock(symbol);
            LocalDate tradingDate = latestTradingDate(stock.stockId());

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("stockId", stock.stockId())
                    .addValue("startTime", Timestamp.valueOf(tradingDate.atTime(start)))
                    .addValue("endTime", Timestamp.valueOf(tradingDate.atTime(end)));

            Long totalVolume = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(interval_volume), 0) AS total_volume
                    FROM stock_price
                    WHERE stock_id = :stockId
                      AND price_time >= :startTime
                      AND price_time <= :endTime
                    """, params, (rs, rowNum) -> rs.getLong("total_volume"));

            return """
                    Volume for %s on %s between %s and %s:
                    - Total volume: %s
                    """.formatted(
                    stock.symbol(),
                    tradingDate,
                    start,
                    end,
                    totalVolume == null ? 0 : totalVolume);
        }

        String normalizedSector = sector == null || sector.isBlank() ? null : sector.trim();
        String sql = """
                SELECT s.symbol, COALESCE(SUM(sp.interval_volume), 0) AS total_volume
                FROM stock s
                JOIN LATERAL (
                    SELECT CAST(MAX(price_time) AS DATE) AS trading_date
                    FROM stock_price
                    WHERE stock_id = s.stock_id
                ) trade_date ON TRUE
                JOIN stock_price sp
                  ON sp.stock_id = s.stock_id
                 AND DATE(sp.price_time) = trade_date.trading_date
                """ + (normalizedSector == null ? "" : "WHERE LOWER(s.sector) = LOWER(:sector)\n") + """
                GROUP BY s.symbol
                ORDER BY total_volume DESC
                LIMIT 5
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (normalizedSector != null) {
            params.addValue("sector", normalizedSector);
        }

        List<VolumeRow> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> new VolumeRow(
                rs.getString("symbol"),
                rs.getLong("total_volume")));

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    normalizedSector == null ? "No volume data found." : "No volume data found for sector " + normalizedSector + ".");
        }

        StringBuilder answer = new StringBuilder();
        answer.append(normalizedSector == null ? "Top volume stocks:\n" : "Top volume stocks in " + normalizedSector + " sector:\n");
        for (VolumeRow row : rows) {
            answer.append("- ").append(row.symbol()).append(": ").append(row.totalVolume()).append('\n');
        }
        return answer.toString().trim();
    }

    private StockRow findStock(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A stock symbol is required for this market query.");
        }

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("symbol", symbol.toUpperCase());
        List<StockRow> rows = jdbcTemplate.query("""
                SELECT stock_id, symbol, name
                FROM stock
                WHERE UPPER(symbol) = :symbol
                LIMIT 1
                """, params, (rs, rowNum) -> new StockRow(
                rs.getLong("stock_id"),
                rs.getString("symbol"),
                rs.getString("name")));

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No stock found for symbol " + symbol.toUpperCase());
        }
        return rows.get(0);
    }

    private LocalDate latestTradingDate(Long stockId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("stockId", stockId);
        List<LocalDate> rows = jdbcTemplate.query("""
                SELECT CAST(MAX(price_time) AS DATE) AS trading_date
                FROM stock_price
                WHERE stock_id = :stockId
                """, params, (rs, rowNum) -> {
            java.sql.Date date = rs.getDate("trading_date");
            return date == null ? null : date.toLocalDate();
        });

        LocalDate tradingDate = rows.isEmpty() ? null : rows.get(0);
        if (tradingDate == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No price history found for the requested stock.");
        }
        return tradingDate;
    }

    private PriceRow findCandle(Long stockId, LocalDate tradingDate, LocalTime requestedTime) {
        LocalDateTime start = tradingDate.atTime(requestedTime);
        LocalDateTime end = start.plusMinutes(5);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("stockId", stockId)
                .addValue("startTime", Timestamp.valueOf(start))
                .addValue("endTime", Timestamp.valueOf(end));

        List<PriceRow> rows = jdbcTemplate.query("""
                SELECT price_time, interval_close, interval_volume
                FROM stock_price
                WHERE stock_id = :stockId
                  AND price_time >= :startTime
                  AND price_time < :endTime
                ORDER BY price_time ASC
                LIMIT 1
                """, params, PRICE_ROW_MAPPER);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No candle found for %s on %s.".formatted(requestedTime, tradingDate));
        }
        return rows.get(0);
    }

    private PriceWithStock findLatestPrice(String symbol) {
        StockRow stock = findStock(symbol);

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("stockId", stock.stockId());
        List<PriceWithStock> rows = jdbcTemplate.query("""
                SELECT interval_close, interval_volume
                FROM stock_price
                WHERE stock_id = :stockId
                ORDER BY price_time DESC
                LIMIT 1
                """, params, (rs, rowNum) -> new PriceWithStock(
                stock.symbol(),
                rs.getBigDecimal("interval_close"),
                getNullableLong(rs, "interval_volume")));

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No latest price found for symbol " + stock.symbol());
        }
        return rows.get(0);
    }

    private LocalTime parseTime(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid HH:mm time is required for this market query.");
        }
        return LocalTime.parse(time);
    }

    private static final RowMapper<PriceRow> PRICE_ROW_MAPPER = (rs, rowNum) -> new PriceRow(
            rs.getTimestamp("price_time") == null ? null : rs.getTimestamp("price_time").toLocalDateTime(),
            rs.getBigDecimal("interval_close"),
            getNullableLong(rs, "interval_volume"));

    private static Long getNullableLong(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record StockRow(Long stockId, String symbol, String name) {
    }

    private record PriceRow(LocalDateTime priceTime, BigDecimal intervalClose, Long intervalVolume) {
    }

    private record PriceWithStock(String symbol, BigDecimal price, Long volume) {
    }

    private record SectorMoverRow(String symbol, BigDecimal latestPrice, BigDecimal openPrice, BigDecimal pctChange) {
    }

    private record VolumeRow(String symbol, Long totalVolume) {
    }
}
