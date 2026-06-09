package com.stockasticappbackend.websocket;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.service.stockprice.StockPriceService;

import lombok.extern.slf4j.Slf4j;

/**
 * Native (non-STOMP) WebSocket handler that streams the latest price for a
 * single stock to the frontend. Clients connect to {@code /ws/stocks?stockId=<id>}
 * and receive a JSON {@link StockPriceResponse} immediately and then on a fixed
 * interval. This mirrors the contract the reactive market-data service exposed,
 * implemented in blocking style for the monolith.
 */
@Component
@Slf4j
public class StockPriceNativeWebSocketHandler extends TextWebSocketHandler {

    private static final CloseStatus MISSING_STOCK_STATUS = new CloseStatus(4400, "Missing stockId");

    private final StockPriceService stockPriceService;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;
    private final long liveUpdateIntervalMillis;

    private final Map<String, ScheduledFuture<?>> sessionTasks = new ConcurrentHashMap<>();

    public StockPriceNativeWebSocketHandler(
            StockPriceService stockPriceService,
            ObjectMapper objectMapper,
            @Qualifier("webSocketTaskScheduler") TaskScheduler taskScheduler,
            @Value("${stockprice.live-update.interval-ms:3000}") long liveUpdateIntervalMillis) {
        this.stockPriceService = stockPriceService;
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler;
        this.liveUpdateIntervalMillis = liveUpdateIntervalMillis;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long stockId = resolveStockId(session);
        if (stockId == null) {
            session.close(MISSING_STOCK_STATUS);
            return;
        }

        log.info("Opened native stock websocket for stock {} and session {}", stockId, session.getId());

        // Immediate snapshot, then periodic updates.
        sendPriceUpdate(session, stockId);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> sendPriceUpdate(session, stockId),
                java.time.Duration.ofMillis(liveUpdateIntervalMillis));
        sessionTasks.put(session.getId(), future);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ScheduledFuture<?> future = sessionTasks.remove(session.getId());
        if (future != null) {
            future.cancel(true);
        }
        log.info("Closed native stock websocket for session {}", session.getId());
    }

    private void sendPriceUpdate(WebSocketSession session, Long stockId) {
        if (!session.isOpen()) {
            ScheduledFuture<?> future = sessionTasks.remove(session.getId());
            if (future != null) {
                future.cancel(true);
            }
            return;
        }

        try {
            StockPriceResponse response = stockPriceService.getLatestPrice(stockId);
            if (response == null) {
                return;
            }
            String payload = objectMapper.writeValueAsString(response);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to send price update for stock {} on session {}: {}",
                    stockId, session.getId(), e.getMessage());
        }
    }

    private Long resolveStockId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String stockIdParam = UriComponentsBuilder.fromUri(uri).build()
                .getQueryParams().getFirst("stockId");
        if (stockIdParam == null || stockIdParam.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(stockIdParam.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
