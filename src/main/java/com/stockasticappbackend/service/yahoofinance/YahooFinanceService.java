package com.stockasticappbackend.service.yahoofinance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to fetch stock quotes from Yahoo Finance API.
 * Handles API requests with retry logic for reliability.
 */
@Service
@Slf4j
public class YahooFinanceService {

    @Value("${yahoo.finance.api.url}")
    private String yahooApiUrl;

    @Value("${yahoo.finance.api.timeout}")
    private int apiTimeoutSeconds;

    private final HttpClient httpClient;
    private final YahooResponseParser responseParser;

    public YahooFinanceService(YahooResponseParser responseParser) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.responseParser = responseParser;
    }

    /**
     * Fetches current quote for a stock symbol with retry logic.
     */
    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public YahooQuote getQuote(String symbol) {
        try {
            String yahooSymbol = getYahooSymbol(symbol);
            return fetchQuote(yahooSymbol, symbol);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch quote for " + symbol, e);
        }
    }

    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public YahooQuote getQuoteForResolvedSymbol(String yahooSymbol, String displaySymbol) {
        try {
            return fetchQuote(yahooSymbol, displaySymbol);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch quote for " + displaySymbol, e);
        }
    }

    @Recover
    public YahooQuote recoverQuote(Exception e, String symbol) {
        log.error("Failed to fetch quote for {} after retries: {}", symbol, e.getMessage());
        return null;
    }

    /**
     * Fetches intraday 5-minute interval data for a stock with retry logic.
     */
    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<YahooQuote> getIntradayQuotes(String symbol) {
        try {
            String yahooSymbol = getYahooSymbol(symbol);
            return fetchIntradayQuotes(yahooSymbol, symbol, "1d");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch intraday quotes for " + symbol, e);
        }
    }

    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<YahooQuote> getIntradayQuotesForResolvedSymbol(String yahooSymbol, String displaySymbol) {
        try {
            return fetchIntradayQuotes(yahooSymbol, displaySymbol, "1d");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch intraday quotes for " + displaySymbol, e);
        }
    }

    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<YahooQuote> getExtendedIntradayQuotesForResolvedSymbol(String yahooSymbol, String displaySymbol, String range) {
        try {
            return fetchIntradayQuotes(yahooSymbol, displaySymbol, range);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch extended intraday quotes for " + displaySymbol, e);
        }
    }

    /**
     * Fetches intraday 5-minute interval data for a stock for a specific range.
     */
    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<YahooQuote> getExtendedIntradayQuotes(String symbol, String range) {
        try {
            String yahooSymbol = getYahooSymbol(symbol);
            return fetchIntradayQuotes(yahooSymbol, symbol, range);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch extended intraday quotes for " + symbol, e);
        }
    }

    /**
     * Fetches historical chart data for different time ranges.
     * Data is returned directly, not saved to database.
     */
    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ChartData getHistoricalData(String symbol, String range) {
        try {
            String yahooSymbol = getYahooSymbol(symbol);
            return fetchHistoricalData(yahooSymbol, symbol, range);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch historical data for " + symbol, e);
        }
    }

    @Retryable(
        retryFor = { IOException.class, InterruptedException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ChartData getHistoricalDataForResolvedSymbol(String yahooSymbol, String displaySymbol, String range) {
        try {
            return fetchHistoricalData(yahooSymbol, displaySymbol, range);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch historical data for " + displaySymbol, e);
        }
    }

    @Recover
    public ChartData recoverHistoricalData(Exception e, String symbol, String range) {
        log.error("Failed to fetch historical data for {} after retries: {}", symbol, e.getMessage());
        return new ChartData(symbol, range, mapRangeToYahooParams(range).interval, new ArrayList<>());
    }

    // Helper method to build HTTP request with common headers
    private HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(apiTimeoutSeconds))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private YahooQuote fetchQuote(String yahooSymbol, String displaySymbol) throws IOException, InterruptedException {
        String url = yahooApiUrl + yahooSymbol + "?interval=1d&range=1d";
        log.debug("Fetching quote from: {}", url);

        HttpRequest request = buildRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return responseParser.parseQuoteResponse(response.body(), displaySymbol);
        } else if (response.statusCode() == 429) {
            log.warn("Rate limited by Yahoo Finance for {}. Retrying...", displaySymbol);
            throw new RuntimeException("Rate limited: 429");
        } else {
            log.warn("Yahoo Finance API returned status {} for symbol {}", response.statusCode(), displaySymbol);
            return null;
        }
    }

    private List<YahooQuote> fetchIntradayQuotes(String yahooSymbol, String displaySymbol, String range)
            throws IOException, InterruptedException {
        String url = yahooApiUrl + yahooSymbol + "?interval=5m&range=" + range;
        log.debug("Fetching intraday data ({}) from: {}", range, url);

        HttpRequest request = buildRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            List<YahooQuote> quotes = responseParser.parseIntradayResponse(response.body(), displaySymbol);
            log.info("Fetched {} intraday quotes for {} (range: {})", quotes.size(), displaySymbol, range);
            return quotes;
        } else if (response.statusCode() == 429) {
            log.warn("Rate limited by Yahoo Finance for {}. Retrying...", displaySymbol);
            throw new RuntimeException("Rate limited: 429");
        } else {
            log.warn("Yahoo Finance API returned status {} for symbol {}", response.statusCode(), displaySymbol);
            return new ArrayList<>();
        }
    }

    private ChartData fetchHistoricalData(String yahooSymbol, String displaySymbol, String range)
            throws IOException, InterruptedException {
        RangeConfig config = mapRangeToYahooParams(range);
        String url = yahooApiUrl + yahooSymbol + "?interval=" + config.interval + "&range=" + config.yahooRange;
        log.debug("Fetching historical data from: {}", url);

        HttpRequest request = buildRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ChartData chartData = responseParser.parseHistoricalResponse(response.body(), displaySymbol, range, config.interval);
            log.info("Fetched {} data points for {} ({})", chartData.getDataPoints().size(), displaySymbol, range);
            return chartData;
        } else if (response.statusCode() == 429) {
            log.warn("Rate limited by Yahoo Finance for {}. Retrying...", displaySymbol);
            throw new RuntimeException("Rate limited: 429");
        } else {
            log.warn("Yahoo Finance API returned status {} for symbol {}", response.statusCode(), displaySymbol);
            return new ChartData(displaySymbol, range, config.interval, new ArrayList<>());
        }
    }

    // Helper method to convert symbol to Yahoo Finance format
    private String getYahooSymbol(String symbol) {
        if (!symbol.contains(".")) {
            return symbol + ".NS";
        }
        return symbol;
    }

    // Helper method to map range to Yahoo Finance parameters
    private RangeConfig mapRangeToYahooParams(String range) {
        return switch (range.toUpperCase()) {
            case "1W" -> new RangeConfig("5d", "1d");
            case "1M" -> new RangeConfig("1mo", "1wk");
            case "3M" -> new RangeConfig("3mo", "1mo");
            case "1Y" -> new RangeConfig("1y", "1mo");
            case "3Y" -> new RangeConfig("3y", "1mo");
            default -> new RangeConfig("1mo", "1wk");
        };
    }

    // Simple record for range configuration
    private record RangeConfig(String yahooRange, String interval) {
    }
}
