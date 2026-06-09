package com.stockasticappbackend.service.growwapi;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.service.yahoofinance.YahooQuote;

import lombok.extern.slf4j.Slf4j;

/**
 * Blocking Groww API client used as a fallback price source when Yahoo Finance
 * returns no data. Mirrors the YahooQuote contract so callers can treat both
 * sources interchangeably. Disabled by default; enable via
 * {@code marketdata.groww.fallback.enabled=true}.
 */
@Service
@Slf4j
public class GrowwApiService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final boolean fallbackEnabled;
    private final String debugSymbol;

    public GrowwApiService(
            @Value("${groww.api.base-url:https://groww.in}") String baseUrl,
            @Value("${groww.api.timeout-seconds:10}") int timeoutSeconds,
            @Value("${marketdata.groww.fallback.enabled:false}") boolean fallbackEnabled,
            @Value("${marketdata.groww.debug-symbol:}") String debugSymbol) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.fallbackEnabled = fallbackEnabled;
        this.debugSymbol = debugSymbol == null ? "" : debugSymbol.trim().toUpperCase();
    }

    public List<YahooQuote> getIntradayHistory(Stock stock, int intervalMinutes) {
        if (!fallbackEnabled || stock == null || stock.getSymbol() == null || stock.getSymbol().isBlank()) {
            return List.of();
        }

        String exchange = resolveExchange(stock);
        if (exchange == null) {
            return List.of();
        }

        String symbol = stock.getSymbol().trim().toUpperCase();
        String url = baseUrl + "/v1/api/charting_service/v2/chart/exchange/" + exchange
                + "/segment/CASH/" + symbol + "/daily?intervalInMinutes=" + intervalMinutes;

        try {
            HttpResponse<String> response = httpClient.send(buildRequest(url), HttpResponse.BodyHandlers.ofString());
            maybeLogDebug(stock, url, response.statusCode(), response.body(), "history");
            if (response.statusCode() == 200) {
                return parseHistoryResponse(response.body(), symbol);
            }
            log.warn("Groww history request failed for {}:{} with status {}", exchange, symbol, response.statusCode());
        } catch (IOException | InterruptedException e) {
            log.warn("Groww history request failed for {}:{}: {}", exchange, symbol, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return List.of();
    }

    public YahooQuote getLatestSnapshot(Stock stock) {
        if (!fallbackEnabled || stock == null || stock.getSymbol() == null || stock.getSymbol().isBlank()) {
            return null;
        }

        String exchange = resolveExchange(stock);
        if (exchange == null) {
            return null;
        }

        String symbol = stock.getSymbol().trim().toUpperCase();
        String url = baseUrl + "/v1/api/stocks_data/v1/tr_live_prices/exchange/" + exchange
                + "/segment/CASH/" + symbol + "/latest";

        try {
            HttpResponse<String> response = httpClient.send(buildRequest(url), HttpResponse.BodyHandlers.ofString());
            maybeLogDebug(stock, url, response.statusCode(), response.body(), "latest");
            if (response.statusCode() == 200) {
                return parseLatestResponse(response.body(), symbol);
            }
            log.warn("Groww latest request failed for {}:{} with status {}", exchange, symbol, response.statusCode());
        } catch (IOException | InterruptedException e) {
            log.warn("Groww latest request failed for {}:{}: {}", exchange, symbol, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return null;
    }

    private HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
    }

    private String resolveExchange(Stock stock) {
        String exchange = stock.getExchange() == null ? "" : stock.getExchange().trim().toUpperCase();
        return switch (exchange) {
            case "NSE", "BSE" -> exchange;
            default -> null;
        };
    }

    private List<YahooQuote> parseHistoryResponse(String body, String displaySymbol) {
        List<YahooQuote> quotes = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode candles = root.path("candles");
            if (!candles.isArray()) {
                return quotes;
            }

            for (JsonNode candle : candles) {
                if (!candle.isArray() || candle.size() < 6) {
                    continue;
                }

                long epochSeconds = candle.get(0).asLong(0L);
                if (epochSeconds <= 0L) {
                    continue;
                }

                LocalDateTime priceTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), IST_ZONE);
                LocalTime localTime = priceTime.toLocalTime();
                if (localTime.isBefore(MARKET_OPEN) || localTime.isAfter(MARKET_CLOSE.plusMinutes(5))) {
                    continue;
                }

                BigDecimal open = decimalValue(candle.get(1));
                BigDecimal high = decimalValue(candle.get(2));
                BigDecimal low = decimalValue(candle.get(3));
                BigDecimal close = decimalValue(candle.get(4));
                Long volume = longValue(candle.get(5));

                if (close == null) {
                    continue;
                }

                quotes.add(YahooQuote.builder()
                        .symbol(displaySymbol)
                        .price(close)
                        .openPrice(open)
                        .volume(volume)
                        .intervalOpen(open)
                        .intervalHigh(high)
                        .intervalLow(low)
                        .intervalClose(close)
                        .intervalVolume(volume)
                        .priceTime(priceTime)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse Groww history for {}: {}", displaySymbol, e.getMessage());
        }

        quotes.sort(Comparator.comparing(YahooQuote::getPriceTime));
        return quotes;
    }

    private YahooQuote parseLatestResponse(String body, String displaySymbol) {
        try {
            JsonNode root = objectMapper.readTree(body);
            BigDecimal ltp = decimalValue(root.path("ltp"));
            if (ltp == null) {
                return null;
            }

            long tsSeconds = root.path("tsInMillis").asLong(0L) / 1000L;
            if (tsSeconds <= 0L) {
                tsSeconds = root.path("lastTradeTime").asLong(0L);
            }
            LocalDateTime priceTime = tsSeconds > 0L
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(tsSeconds), IST_ZONE)
                    : LocalDateTime.now(IST_ZONE);

            return YahooQuote.builder()
                    .symbol(displaySymbol)
                    .price(ltp)
                    .previousClose(decimalValue(root.path("close")))
                    .openPrice(decimalValue(root.path("open")))
                    .dayHigh(decimalValue(root.path("high")))
                    .dayLow(decimalValue(root.path("low")))
                    .fiftyTwoWeekHigh(decimalValue(root.path("yearHighPrice")))
                    .fiftyTwoWeekLow(decimalValue(root.path("yearLowPrice")))
                    .volume(longValue(root.path("volume")))
                    .intervalOpen(ltp)
                    .intervalHigh(ltp)
                    .intervalLow(ltp)
                    .intervalClose(ltp)
                    .intervalVolume(longValue(root.path("volume")))
                    .priceTime(priceTime)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Groww latest for {}: {}", displaySymbol, e.getMessage());
            return null;
        }
    }

    private void maybeLogDebug(Stock stock, String url, int statusCode, String responseBody, String kind) {
        if (debugSymbol.isBlank() || stock == null || stock.getSymbol() == null || !debugSymbol.equalsIgnoreCase(stock.getSymbol())) {
            return;
        }

        String compactBody = responseBody == null ? "" : responseBody.replaceAll("\\s+", " ");
        if (compactBody.length() > 800) {
            compactBody = compactBody.substring(0, 800) + "...";
        }

        log.info("Groww debug {} for {} -> status={}, url={}", kind, stock.getSymbol(), statusCode, url);
        log.info("Groww debug body {} for {}: {}", kind, stock.getSymbol(), compactBody);
    }

    private BigDecimal decimalValue(JsonNode node) {
        if (node != null && !node.isNull() && (node.isNumber() || node.isTextual())) {
            String text = node.asText();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                try {
                    return new BigDecimal(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Long longValue(JsonNode node) {
        if (node != null && !node.isNull() && (node.isNumber() || node.isTextual())) {
            String text = node.asText();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                try {
                    return Long.parseLong(text.contains(".") ? text.substring(0, text.indexOf('.')) : text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
