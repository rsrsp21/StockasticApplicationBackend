package com.stockasticappbackend.websocket;

import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.stockasticappbackend.security.jwt.JwtUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Native (non-STOMP) notification WebSocket handler. Authenticates the JWT
 * supplied via the {@code access_token}/{@code token} query parameter (or
 * Authorization header), then registers the session so the notification
 * service can push live updates to the user.
 */
@Component
@Slf4j
public class NotificationNativeWebSocketHandler extends TextWebSocketHandler {

    private static final CloseStatus UNAUTHORIZED_CLOSE_STATUS = new CloseStatus(4401, "Unauthorized");
    private static final String USER_EMAIL_ATTRIBUTE = "userEmail";

    private final JwtUtil jwtUtil;
    private final NotificationSessionRegistry sessionRegistry;

    public NotificationNativeWebSocketHandler(JwtUtil jwtUtil, NotificationSessionRegistry sessionRegistry) {
        this.jwtUtil = jwtUtil;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userEmail = authenticate(session);
        if (userEmail == null) {
            log.warn("Rejected notification websocket session {} due to missing/invalid token", session.getId());
            session.close(UNAUTHORIZED_CLOSE_STATUS);
            return;
        }

        session.getAttributes().put(USER_EMAIL_ATTRIBUTE, userEmail);
        sessionRegistry.register(userEmail, session);
        log.info("Opened notification websocket for user {} and session {}", userEmail, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userEmail = session.getAttributes().get(USER_EMAIL_ATTRIBUTE);
        if (userEmail != null) {
            sessionRegistry.unregister((String) userEmail, session.getId());
            log.info("Closed notification websocket for user {} and session {}", userEmail, session.getId());
        }
    }

    private String authenticate(WebSocketSession session) {
        String token = resolveToken(session);
        if (token == null) {
            return null;
        }
        try {
            String username = jwtUtil.extractUsername(token);
            return (username != null && !username.isBlank()) ? username : null;
        } catch (Exception e) {
            log.warn("Rejected notification websocket session {} due to invalid token: {}", session.getId(), e.getMessage());
            return null;
        }
    }

    private String resolveToken(WebSocketSession session) {
        // Read only from the handshake URI. The underlying servlet request has
        // already been recycled by the time this runs, so handshake headers are
        // not safe to access here. The frontend sends the JWT as a query param.
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String token = queryParams.getFirst("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        String accessToken = queryParams.getFirst("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }
        return null;
    }
}
