package com.stockasticappbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.stockasticappbackend.security.jwt.WebSocketTokenFilter;

/**
 * Configuration class for WebSocket messaging with STOMP protocol.
 * Enables real-time stock price updates to connected clients via WebSocket.
 * Clients subscribe to topic destinations to receive live price data.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker for WebSocket communication.
     * Enables a simple in-memory message broker for broadcasting messages
     * to subscribed clients on "/topic" destinations. Client messages are
     * routed through "/app" prefix for server-side processing.
     *
     * @param registry The MessageBrokerRegistry to configure.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Autowired
    private WebSocketTokenFilter webSocketTokenFilter;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketTokenFilter);
    }

    /**
     * Registers STOMP WebSocket endpoints.
     * Exposes the "/ws/stocks" endpoint for WebSocket connections.
     * SockJS fallback is enabled for browsers without native WebSocket support.
     *
     * @param registry The StompEndpointRegistry to configure.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/stomp/stocks")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
