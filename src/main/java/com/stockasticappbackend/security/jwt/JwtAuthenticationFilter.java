package com.stockasticappbackend.security.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stockasticappbackend.security.service.CustomUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT authentication filter for processing requests.
 * Intercepts incoming requests to extract and validate JWT tokens
 * from the Authorization header. Valid tokens result in setting
 * the authentication in the SecurityContext.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	@Autowired
    private JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Filters each request to check for JWT authentication.
     * Extracts the Bearer token from the Authorization header,
     * validates it, and sets the authentication context if valid.
     * If the token is expired or invalid, the request proceeds
     * unauthenticated so Spring Security returns a proper 401.
     *
     * @param request     The HTTP request.
     * @param response    The HTTP response.
     * @param filterChain The filter chain.
     * @throws ServletException If a servlet error occurs.
     * @throws IOException      If an I/O error occurs.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String username = jwtUtil.extractUsername(token);

                if (username != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtUtil.validateToken(token, userDetails)) {

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext()
                                .setAuthentication(authToken);
                    }
                }
            } catch (ExpiredJwtException e) {
                log.debug("JWT token expired for request: {} {}", request.getMethod(), request.getRequestURI());
                // Don't set authentication — Spring Security will return 401
            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                // Don't set authentication — Spring Security will return 401
            }
        }

        filterChain.doFilter(request, response);
    }
}