package com.stockasticappbackend.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockasticappbackend.dto.notification.NotificationResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Tracks open native notification WebSocket sessions per user (by email) and
 * pushes notification payloads to them in real time.
 */
@Component
@Slf4j
public class NotificationSessionRegistry {

    private final Map<String, Map<String, WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String userEmail, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userEmail, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    public void unregister(String userEmail, String sessionId) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userEmail);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userEmail);
            }
        }
    }

    public void send(String userEmail, NotificationResponse response) {
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userEmail);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Failed to serialize notification for {}: {}", userEmail, e.getMessage());
            return;
        }

        for (WebSocketSession session : sessions.values()) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to push notification to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
