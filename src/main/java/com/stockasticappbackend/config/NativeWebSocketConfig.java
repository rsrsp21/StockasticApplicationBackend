package com.stockasticappbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.stockasticappbackend.websocket.NotificationNativeWebSocketHandler;
import com.stockasticappbackend.websocket.StockPriceNativeWebSocketHandler;

/**
 * Registers the native (non-STOMP) stock-price WebSocket endpoint consumed by
 * the frontend at {@code /ws/stocks?stockId=<id>}. The legacy STOMP endpoint
 * lives on a separate path so both mechanisms can coexist.
 */
@Configuration
@EnableWebSocket
public class NativeWebSocketConfig implements WebSocketConfigurer {

    private final StockPriceNativeWebSocketHandler stockPriceNativeWebSocketHandler;
    private final NotificationNativeWebSocketHandler notificationNativeWebSocketHandler;

    public NativeWebSocketConfig(
            StockPriceNativeWebSocketHandler stockPriceNativeWebSocketHandler,
            NotificationNativeWebSocketHandler notificationNativeWebSocketHandler) {
        this.stockPriceNativeWebSocketHandler = stockPriceNativeWebSocketHandler;
        this.notificationNativeWebSocketHandler = notificationNativeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stockPriceNativeWebSocketHandler, "/ws/stocks")
                .setAllowedOriginPatterns("*");
        registry.addHandler(notificationNativeWebSocketHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");
    }
}
