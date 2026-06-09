package com.stockasticappbackend.service.yahoofinance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Parser for Yahoo Finance API responses. Handles JSON parsing for quotes,
 * intraday data, and historical data.
 */
@Component
@Slf4j
public class YahooResponseParser {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private final ObjectMapper objectMapper;

    public YahooResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses a single quote response from Yahoo Finance.
     */
    public YahooQuote parseQuoteResponse(String responseBody, String symbol) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode chart = root.path("chart");
            JsonNode result = chart.path("result");

            if (result.isArray() && result.size() > 0) {
                JsonNode firstResult = result.get(0);
                JsonNode meta = firstResult.path("meta");
                JsonNode indicators = firstResult.path("indicators");
                JsonNode quote = indicators.path("quote");

                BigDecimal price = new BigDecimal(meta.path("regularMarketPrice").asText("0"));
                BigDecimal previousClose = new BigDecimal(meta.path("chartPreviousClose").asText("0"));
                BigDecimal dayHigh = getDecimalValue(meta.path("regularMarketDayHigh"));
                BigDecimal dayLow = getDecimalValue(meta.path("regularMarketDayLow"));
                BigDecimal fiftyTwoWeekHigh = getDecimalValue(meta.path("fiftyTwoWeekHigh"));
                BigDecimal fiftyTwoWeekLow = getDecimalValue(meta.path("fiftyTwoWeekLow"));
                Long volume = meta.path("regularMarketVolume").asLong(0);

                // Strict quote mode for websocket/latest quote path:
                // prefer only meta.regularMarket* values (no array override).
                return YahooQuote.builder()
                        .symbol(symbol)
                        .price(price)
                        .previousClose(previousClose)
                        .openPrice(getDecimalValue(meta.path("regularMarketOpen")))
                        .dayHigh(dayHigh)
                        .dayLow(dayLow)
                        .fiftyTwoWeekHigh(fiftyTwoWeekHigh)
                        .fiftyTwoWeekLow(fiftyTwoWeekLow)
                        .volume(volume)
                        .intervalOpen(price)
                        .intervalHigh(price)
                        .intervalLow(price)
                        .intervalClose(price)
                        .intervalVolume(volume)
                        .priceTime(LocalDateTime.now(IST_ZONE))
                        .build();
            }

            log.warn("Could not parse Yahoo Finance response for symbol: {}", symbol);
            return null;

        } catch (Exception e) {
            log.error("Error parsing Yahoo Finance response for symbol: {}", symbol, e);
            return null;
        }
    }

    /**
     * Parses intraday 5-minute interval data from Yahoo Finance. Filters data
     * to only include market hours (9:15 AM - 3:30 PM IST).
     */
    public List<YahooQuote> parseIntradayResponse(String responseBody, String symbol) {
        List<YahooQuote> quotes = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode chart = root.path("chart");
            JsonNode result = chart.path("result");

            if (result.isArray() && result.size() > 0) {
                JsonNode firstResult = result.get(0);
                JsonNode meta = firstResult.path("meta");
                JsonNode timestamps = firstResult.path("timestamp");
                JsonNode indicators = firstResult.path("indicators");
                JsonNode quote = indicators.path("quote");

                BigDecimal previousClose = new BigDecimal(meta.path("chartPreviousClose").asText("0"));
                if (previousClose.compareTo(BigDecimal.ZERO) == 0) {
                    previousClose = new BigDecimal(meta.path("previousClose").asText("0"));
                }
                BigDecimal dayHigh = getDecimalValue(meta.path("regularMarketDayHigh"));
                BigDecimal dayLow = getDecimalValue(meta.path("regularMarketDayLow"));
                BigDecimal fiftyTwoWeekHigh = getDecimalValue(meta.path("fiftyTwoWeekHigh"));
                BigDecimal fiftyTwoWeekLow = getDecimalValue(meta.path("fiftyTwoWeekLow"));

                if (timestamps.isArray() && quote.isArray() && quote.size() > 0) {
                    JsonNode quoteData = quote.get(0);
                    JsonNode opens = quoteData.path("open");
                    JsonNode highs = quoteData.path("high");
                    JsonNode lows = quoteData.path("low");
                    JsonNode closes = quoteData.path("close");
                    JsonNode volumes = quoteData.path("volume");

                    int dataPoints = timestamps.size();
                    log.debug("Found {} raw data points for {}", dataPoints, symbol);

                    for (int i = 0; i < dataPoints; i++) {
                        try {
                            long timestamp = timestamps.get(i).asLong();
                            LocalDateTime priceTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(timestamp), IST_ZONE);

                            // Filter: Only include data within market hours (9:15 - 3:30)
                            // We allow up to 3:35 to strictly ensure we capture the 3:30 closing candle
                            LocalTime time = priceTime.toLocalTime();

                            // Apply market hours filter ONLY for Indian stocks
                            if (symbol.endsWith(".NS") || symbol.endsWith(".BO")) {
                                if (time.isBefore(MARKET_OPEN) || time.isAfter(MARKET_CLOSE.plusMinutes(5))) {
                                    log.trace("Skipping data point at {} (outside market hours)", priceTime);
                                    continue;
                                }
                            }

                            BigDecimal intervalOpen = getValueAt(opens, i);
                            BigDecimal intervalHigh = getValueAt(highs, i);
                            BigDecimal intervalLow = getValueAt(lows, i);
                            BigDecimal intervalClose = getValueAt(closes, i);
                            Long intervalVolume = getVolumeAt(volumes, i);

                            if (intervalClose != null) {
                                YahooQuote quoteObj = YahooQuote.builder()
                                        .symbol(symbol)
                                        .price(intervalClose)
                                        .previousClose(previousClose)
                                        .openPrice(intervalOpen)
                                        .dayHigh(dayHigh)
                                        .dayLow(dayLow)
                                        .fiftyTwoWeekHigh(fiftyTwoWeekHigh)
                                        .fiftyTwoWeekLow(fiftyTwoWeekLow)
                                        .volume(intervalVolume)
                                        .intervalOpen(intervalOpen)
                                        .intervalHigh(intervalHigh)
                                        .intervalLow(intervalLow)
                                        .intervalClose(intervalClose)
                                        .intervalVolume(intervalVolume)
                                        .priceTime(priceTime)
                                        .build();
                                quotes.add(quoteObj);
                            }
                        } catch (Exception e) {
                            log.trace("Skipping invalid data point {} for {}", i, symbol);
                        }
                    }

                    log.info("Filtered to {} data points within market hours for {}", quotes.size(), symbol);

                    // EDGE CASE: If chart data ends at 15:25 but meta has 15:30 closing price, add
                    // it.
                    // This creates the 76th row and ensures accurate closing price.
                    try {
                        long regularMarketTime = meta.path("regularMarketTime").asLong(0);
                        BigDecimal regularMarketPrice = new BigDecimal(meta.path("regularMarketPrice").asText("0"));

                        if (regularMarketTime > 0 && regularMarketPrice.compareTo(BigDecimal.ZERO) > 0) {
                            LocalDateTime regularTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(regularMarketTime), IST_ZONE);

                            // Check if this time is valid (within market hours + buffer)
                            LocalTime time = regularTime.toLocalTime();
                            boolean isValidTime = !time.isBefore(MARKET_OPEN)
                                    && !time.isAfter(MARKET_CLOSE.plusMinutes(5));

                            if (isValidTime) {
                                boolean isNewer = quotes.isEmpty()
                                        || regularTime.isAfter(quotes.get(quotes.size() - 1).getPriceTime());

                                if (isNewer) {
                                    log.debug("Appending meta price tick at {} (Latest: {})",
                                            regularTime, regularMarketPrice);

                                    // FIX: Frontend expects exactly 15:30:00 for the closing candle.
                                    // If this is the "final" closing tick (which it is, coming from meta),
                                    // snap its time to exactly 15:30:00 to ensure chart compatibility.
                                    LocalDateTime snappedCloseTime = regularTime.toLocalDate().atTime(MARKET_CLOSE);

                                    YahooQuote latestTick = YahooQuote.builder()
                                            .symbol(symbol)
                                            .price(regularMarketPrice)
                                            .previousClose(previousClose)
                                            .openPrice(regularMarketPrice)
                                            .dayHigh(dayHigh != null ? dayHigh : regularMarketPrice)
                                            .dayLow(dayLow != null ? dayLow : regularMarketPrice)
                                            .fiftyTwoWeekHigh(fiftyTwoWeekHigh)
                                            .fiftyTwoWeekLow(fiftyTwoWeekLow)
                                            .volume(0L) // Volume is 0 to avoid skewing interval data
                                            .intervalOpen(regularMarketPrice)
                                            .intervalHigh(regularMarketPrice)
                                            .intervalLow(regularMarketPrice)
                                            .intervalClose(regularMarketPrice)
                                            .intervalVolume(0L)
                                            .priceTime(snappedCloseTime)
                                            .build();

                                    quotes.add(latestTick);
                                    log.info("Added live/closing tick for {} at {} (snapped to 15:30:00)", symbol,
                                            snappedCloseTime);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to append meta price tick for {}", symbol, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing intraday response for symbol: {}", symbol, e);
        }

        return quotes;
    }

    /**
     * Parses historical chart data from Yahoo Finance.
     */
    public ChartData parseHistoricalResponse(String responseBody, String symbol, String range, String interval) {
        List<ChartData.DataPoint> dataPoints = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode chart = root.path("chart");
            JsonNode result = chart.path("result");

            if (result.isArray() && result.size() > 0) {
                JsonNode firstResult = result.get(0);
                JsonNode timestamps = firstResult.path("timestamp");
                JsonNode indicators = firstResult.path("indicators");
                JsonNode quote = indicators.path("quote");

                if (timestamps.isArray() && quote.isArray() && quote.size() > 0) {
                    JsonNode quoteData = quote.get(0);
                    JsonNode opens = quoteData.path("open");
                    JsonNode highs = quoteData.path("high");
                    JsonNode lows = quoteData.path("low");
                    JsonNode closes = quoteData.path("close");
                    JsonNode volumes = quoteData.path("volume");

                    for (int i = 0; i < timestamps.size(); i++) {
                        try {
                            long timestamp = timestamps.get(i).asLong();
                            LocalDateTime time = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(timestamp), IST_ZONE);

                            BigDecimal open = getValueAt(opens, i);
                            BigDecimal high = getValueAt(highs, i);
                            BigDecimal low = getValueAt(lows, i);
                            BigDecimal close = getValueAt(closes, i);
                            Long volume = getVolumeAt(volumes, i);

                            if (close != null) {
                                dataPoints.add(ChartData.DataPoint.builder()
                                        .time(time)
                                        .open(open)
                                        .high(high)
                                        .low(low)
                                        .close(close)
                                        .volume(volume)
                                        .build());
                            }
                        } catch (Exception e) {
                            log.trace("Skipping invalid data point {} for {}", i, symbol);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing historical response for symbol: {}", symbol, e);
        }

        return new ChartData(symbol, range, interval, dataPoints);
    }

    // Helper methods for extracting values from JSON arrays
    private BigDecimal getValueAt(JsonNode arrayNode, int index) {
        if (arrayNode.isArray() && index < arrayNode.size()) {
            JsonNode node = arrayNode.get(index);
            if (!node.isNull() && node.isNumber()) {
                return new BigDecimal(node.asText());
            }
        }
        return null;
    }

    private Long getVolumeAt(JsonNode arrayNode, int index) {
        if (arrayNode.isArray() && index < arrayNode.size()) {
            JsonNode node = arrayNode.get(index);
            if (!node.isNull() && node.isNumber()) {
                return node.asLong();
            }
        }
        return null;
    }

    private BigDecimal getLastValue(JsonNode arrayNode) {
        if (arrayNode.isArray() && arrayNode.size() > 0) {
            JsonNode lastNode = arrayNode.get(arrayNode.size() - 1);
            if (!lastNode.isNull() && lastNode.isNumber()) {
                return new BigDecimal(lastNode.asText());
            }
        }
        return null;
    }

    private Long getLastVolume(JsonNode arrayNode) {
        if (arrayNode.isArray() && arrayNode.size() > 0) {
            JsonNode lastNode = arrayNode.get(arrayNode.size() - 1);
            if (!lastNode.isNull() && lastNode.isNumber()) {
                return lastNode.asLong();
            }
        }
        return null;
    }

    private BigDecimal getDecimalValue(JsonNode node) {
        if (node != null && !node.isNull() && node.isNumber()) {
            return new BigDecimal(node.asText());
        }
        return null;
    }
}
