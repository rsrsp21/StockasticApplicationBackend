package com.stockasticappbackend.security.jwt;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.stockasticappbackend.security.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor to handle JWT authentication for WebSocket connections.
 * Extracts the "Authorization" header from the STOMP CONNECT frame.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketTokenFilter implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String username = jwtUtil.extractUsername(token);

                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        if (jwtUtil.validateToken(token, userDetails)) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            
                            accessor.setUser(auth);
                            log.info("Successfully authenticated WebSocket connection for user: {}", username);
                        }
                    }
                } catch (Exception e) {
                    log.error("WebSocket Authentication failed: {}", e.getMessage());
                    // We don't throw exception here to avoid breaking the channel, 
                    // but the user will be null, so subsequent secured subscriptions will fail.
                }
            }
        }
        return message;
    }
}
