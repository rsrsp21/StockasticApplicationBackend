package com.stockasticappbackend.websocket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.github.benmanes.caffeine.cache.Cache;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.event.AutoSellRuleCreatedEvent;
import com.stockasticappbackend.event.PriceAlertCreatedEvent;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.autosell.AutoSellService;
import com.stockasticappbackend.service.pricealert.PriceAlertService;
import com.stockasticappbackend.service.stockprice.MarketHoursService;
import com.stockasticappbackend.service.stockprice.StockPriceService;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;
import com.stockasticappbackend.service.yahoofinance.YahooQuote;
import com.stockasticappbackend.util.StockMarketTypeUtil;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class StockPriceWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final StockPriceService stockPriceService;
    private final YahooFinanceService yahooFinanceService;
    private final MarketHoursService marketHoursService;
    private final StockRepository stockRepository;
    private final PriceAlertService priceAlertService;
    private final AutoSellService autoSellService;
    private final Map<Long, ScheduledFuture<?>> activeStockUpdates = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> updateInProgress = new ConcurrentHashMap<>();
    private final Map<Long, Integer> stockSubscriberCount = new ConcurrentHashMap<>();
    private final Map<Long, String> stockSymbolCache = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> sessionSubscriptionIndex = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastKnownVolumeCache = new ConcurrentHashMap<>();
    private final Map<Long, LivePriceSnapshot> livePriceCache = new ConcurrentHashMap<>();

    // DB is hit at most once per stock ID, because market type does not change at runtime.
    private final Cache<Long, Boolean> internationalStockCache;
    private final TaskScheduler taskScheduler;
    private final long liveUpdateIntervalMillis;

    private static final Pattern TOPIC_PATTERN = Pattern.compile("/topic/prices/(\\d+)");

    public StockPriceWebSocketController(
            SimpMessagingTemplate messagingTemplate,
            StockPriceService stockPriceService,
            YahooFinanceService yahooFinanceService,
            MarketHoursService marketHoursService,
            StockRepository stockRepository,
            PriceAlertService priceAlertService,
            AutoSellService autoSellService,
            Cache<Long, Boolean> internationalStockCache,
            @Qualifier("webSocketTaskScheduler") TaskScheduler taskScheduler,
            @Value("${stockprice.live-update.interval-ms:3000}") long liveUpdateIntervalMillis) {
        this.messagingTemplate = messagingTemplate;
        this.stockPriceService = stockPriceService;
        this.yahooFinanceService = yahooFinanceService;
        this.marketHoursService = marketHoursService;
        this.stockRepository = stockRepository;
        this.priceAlertService = priceAlertService;
        this.autoSellService = autoSellService;
        this.internationalStockCache = internationalStockCache;
        this.taskScheduler = taskScheduler;
        this.liveUpdateIntervalMillis = liveUpdateIntervalMillis;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAlertMonitoring() {
        log.info("Initializing background price monitoring for stocks with active alerts...");
        Set<Long> stockIds = priceAlertService.getStocksWithActiveAlerts();
        for (Long stockId : stockIds) {
            log.info("Starting background monitoring for stock ID (Price Alert): {}", stockId);
            startPriceUpdates(stockId);
        }

        Set<Long> autoSellStockIds = autoSellService.getStocksWithActiveRules();
        for (Long stockId : autoSellStockIds) {
            log.info("Starting background monitoring for stock ID (Auto Sell): {}", stockId);
            startPriceUpdates(stockId);
        }
    }

    @EventListener
    public void handlePriceAlertCreated(PriceAlertCreatedEvent event) {
        log.info("Received new price alert event for stock ID: {}", event.getStockId());
        startPriceUpdates(event.getStockId());
    }

    @EventListener
    public void handleAutoSellRuleCreated(AutoSellRuleCreatedEvent event) {
        log.info("Received new auto-sell rule event for stock ID: {}", event.getStockId());
        startPriceUpdates(event.getStockId());
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (destination == null || sessionId == null) {
            return;
        }

        Matcher matcher = TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long stockId = Long.valueOf(matcher.group(1));
        if (isInternationalStock(stockId)) {
            log.debug("Skipping websocket setup for international stock ID: {}", stockId);
            return;
        }

        log.info("New subscription to stock ID: {} from session: {}", stockId, sessionId);
        sessionSubscriptions.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(stockId);
        if (subscriptionId != null) {
            sessionSubscriptionIndex.computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>())
                    .put(subscriptionId, stockId);
        }
        stockSubscriberCount.merge(stockId, 1, Integer::sum);
        cacheStockSymbol(stockId);
        sendPriceUpdate(stockId);
        if (marketHoursService.isMarketOpen() && !activeStockUpdates.containsKey(stockId)) {
            startPriceUpdates(stockId);
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        if (sessionId == null || subscriptionId == null) {
            log.debug("Unsubscribe event missing session or subscription id");
            return;
        }

        Map<String, Long> subscriptions = sessionSubscriptionIndex.get(sessionId);
        if (subscriptions == null) {
            log.debug("No tracked subscriptions found for session {}", sessionId);
            return;
        }

        Long stockId = subscriptions.remove(subscriptionId);
        if (subscriptions.isEmpty()) {
            sessionSubscriptionIndex.remove(sessionId);
        }
        if (stockId == null) {
            return;
        }

        Set<Long> subscribedStocks = sessionSubscriptions.get(sessionId);
        if (subscribedStocks != null) {
            subscribedStocks.remove(stockId);
            if (subscribedStocks.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }

        decrementSubscribers(stockId);
        log.info("Processed STOMP unsubscribe for stock ID: {} from session: {}", stockId, sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket session disconnected: {}", sessionId);
        Set<Long> subscribedStocks = sessionSubscriptions.remove(sessionId);
        sessionSubscriptionIndex.remove(sessionId);
        if (subscribedStocks != null) {
            for (Long stockId : subscribedStocks) {
                decrementSubscribers(stockId);
                log.debug("Decremented subscriber count for stock {} due to session disconnect", stockId);
            }
        }
    }

    @MessageMapping("/unsubscribe/{stockId}")
    public void handleUnsubscribeMessage(@DestinationVariable Long stockId) {
        log.info("Received unsubscribe message for stock ID: {}", stockId);
        decrementSubscribers(stockId);
    }

    private void cacheStockSymbol(Long stockId) {
        if (!stockSymbolCache.containsKey(stockId)) {
            stockRepository.findById(stockId).ifPresent(stock -> {
                stockSymbolCache.put(stockId, stock.getSymbol());
                log.debug("Cached symbol {} for stock ID {}", stock.getSymbol(), stockId);
            });
        }
    }

    private void decrementSubscribers(Long stockId) {
        int count = stockSubscriberCount.getOrDefault(stockId, 0);
        if (count > 1) {
            stockSubscriberCount.put(stockId, count - 1);
            return;
        }

        stockSubscriberCount.remove(stockId);
        if (!priceAlertService.hasActiveAlerts(stockId) && !autoSellService.hasActiveRules(stockId)) {
            stopPriceUpdates(stockId);
            lastKnownVolumeCache.remove(stockId);
            livePriceCache.remove(stockId);
        } else {
            log.debug("Keeping price updates active for stock {} due to active alerts/rules", stockId);
        }
    }

    private void startPriceUpdates(Long stockId) {
        if (isInternationalStock(stockId)) {
            log.debug("Skipping websocket updates for international stock ID {}", stockId);
            return;
        }

        activeStockUpdates.computeIfAbsent(stockId, key -> {
            log.info("Started live price updates for stock ID: {} (every {} ms)", key, liveUpdateIntervalMillis);
            return taskScheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!marketHoursService.isMarketOpen()) {
                        log.info("Market closed. Stopping live updates for stock ID: {}", key);
                        stopPriceUpdates(key);
                        return;
                    }

                    AtomicBoolean inFlight = updateInProgress.computeIfAbsent(key, id -> new AtomicBoolean(false));
                    if (!inFlight.compareAndSet(false, true)) {
                        return;
                    }
                    sendPriceUpdate(key);

                } catch (Exception e) {
                    log.error("Error sending price update for stock ID: {}", key, e);
                } finally {
                    AtomicBoolean inFlight = updateInProgress.get(key);
                    if (inFlight != null) {
                        inFlight.set(false);
                    }
                }
            }, Duration.ofMillis(liveUpdateIntervalMillis));
        });
    }

    private void stopPriceUpdates(Long stockId) {
        ScheduledFuture<?> future = activeStockUpdates.remove(stockId);
        if (future != null) {
            future.cancel(false);
            log.info("Stopped price updates for stock ID: {}", stockId);
        }
        updateInProgress.remove(stockId);
    }

    private void sendPriceUpdate(Long stockId) {
        try {
            String symbol = stockSymbolCache.get(stockId);
            if (symbol == null) {
                log.warn("No symbol cached for stock ID: {}", stockId);
                cacheStockSymbol(stockId);
                symbol = stockSymbolCache.get(stockId);
                if (symbol == null) {
                    return;
                }
            }

            StockPriceResponse response;
            if (marketHoursService.isMarketOpen()) {
                if (!activeStockUpdates.containsKey(stockId) && stockSubscriberCount.containsKey(stockId)) {
                    startPriceUpdates(stockId);
                }

                YahooQuote quote = getFreshQuoteIfNeeded(stockId, symbol);
                if (quote != null) {
                    BigDecimal changePercent = calculateChangePercent(quote);
                    Long volumeToDisplay = quote.getVolume();
                    if (volumeToDisplay != null && volumeToDisplay > 0) {
                        lastKnownVolumeCache.put(stockId, volumeToDisplay);
                    } else {
                        volumeToDisplay = lastKnownVolumeCache.getOrDefault(stockId, 0L);
                    }

                    response = StockPriceResponse.builder()
                            .stockId(stockId)
                            .symbol(symbol)
                            .price(quote.getPrice())
                            .previousClose(quote.getPreviousClose())
                            .openPrice(quote.getOpenPrice())
                            .dayHigh(quote.getDayHigh())
                            .dayLow(quote.getDayLow())
                            .fiftyTwoWeekHigh(quote.getFiftyTwoWeekHigh())
                            .fiftyTwoWeekLow(quote.getFiftyTwoWeekLow())
                            .volume(volumeToDisplay)
                            .changePercent(changePercent)
                            .priceTime(quote.getPriceTime())
                            .build();
                    livePriceCache.put(stockId, new LivePriceSnapshot(response, Instant.now()));
                } else {
                    response = getLatestLiveOrDbPrice(stockId);
                }
            } else {
                response = getLatestLiveOrDbPrice(stockId);
            }

            if (response == null) {
                return;
            }

            if (response.getVolume() != null && response.getVolume() > 0) {
                lastKnownVolumeCache.put(stockId, response.getVolume());
            }
            messagingTemplate.convertAndSend("/topic/prices/" + stockId, response);
            log.debug("Sent price update for stock {}: {}", symbol, response.getPrice());
            if (response.getPrice() != null) {
                try {
                    priceAlertService.checkAndTriggerAlerts(stockId, response.getPrice());
                    autoSellService.checkAndTriggerRules(stockId, response.getPrice());
                } catch (Exception e) {
                    log.error("Error checking alerts/rules for stock {}: {}", stockId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not send price update for stock ID: {}: {}", stockId, e.getMessage());
        }
    }

    private BigDecimal calculateChangePercent(YahooQuote quote) {
        BigDecimal baselinePrice = quote.getPreviousClose();
        if (baselinePrice == null || baselinePrice.compareTo(BigDecimal.ZERO) == 0) {
            baselinePrice = quote.getOpenPrice();
        }

        if (baselinePrice != null && baselinePrice.compareTo(BigDecimal.ZERO) != 0 && quote.getPrice() != null) {
            BigDecimal change = quote.getPrice().subtract(baselinePrice);
            return change.divide(baselinePrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }
        return null;
    }

    private StockPriceResponse getLatestPriceFromDb(Long stockId) {
        try {
            return stockPriceService.getLatestPrice(stockId);
        } catch (Exception e) {
            log.warn("No price available for stock ID: {}", stockId);
            return null;
        }
    }

    private StockPriceResponse getLatestLiveOrDbPrice(Long stockId) {
        LivePriceSnapshot cached = livePriceCache.get(stockId);
        if (cached != null && cached.response() != null) {
            return cached.response();
        }
        return getLatestPriceFromDb(stockId);
    }

    private YahooQuote getFreshQuoteIfNeeded(Long stockId, String symbol) {
        LivePriceSnapshot cached = livePriceCache.get(stockId);
        if (cached != null && !isLivePriceStale(cached.fetchedAt())) {
            return null;
        }
        return yahooFinanceService.getQuote(symbol);
    }

    private boolean isLivePriceStale(Instant fetchedAt) {
        return fetchedAt == null
                || Duration.between(fetchedAt, Instant.now()).toMillis() >= liveUpdateIntervalMillis;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down WebSocket price scheduler");
        activeStockUpdates.values().forEach(future -> future.cancel(false));
        activeStockUpdates.clear();
        stockSymbolCache.clear();
        stockSubscriberCount.clear();
        sessionSubscriptions.clear();
        sessionSubscriptionIndex.clear();
        lastKnownVolumeCache.clear();
        livePriceCache.clear();
        updateInProgress.clear();
        internationalStockCache.invalidateAll();
    }

    /**
     * Checks if a stock is international using a Caffeine cache.
     * DB is hit at most once per stock ID for the lifetime of the application.
     */
    private boolean isInternationalStock(Long stockId) {
        return internationalStockCache.get(stockId, id ->
                stockRepository.findById(id)
                        .map(StockMarketTypeUtil::isInternational)
                        .orElse(false)
        );
    }

    private record LivePriceSnapshot(StockPriceResponse response, Instant fetchedAt) {
    }
}
