package com.stockasticappbackend.controller;

import static com.stockasticappbackend.util.Constants.ACCOUNT_BLOCKED_CONTACT_HELP;
import static com.stockasticappbackend.util.Constants.LOGOUT_SUCCESS;
import static com.stockasticappbackend.util.Constants.REFRESH_TOKEN_MISSING_IN_COOKIE;
import static com.stockasticappbackend.util.Constants.REFRESH_TOKEN_NOT_IN_DATABASE;
import static com.stockasticappbackend.util.Constants.USER_ACCOUNT_NOT_ACTIVE;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.auth.JwtResponse;
import com.stockasticappbackend.dto.auth.LoginRequest;
import com.stockasticappbackend.dto.auth.TokenRefreshResponse;
import com.stockasticappbackend.exception.TokenRefreshException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.RefreshToken;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.security.jwt.JwtUtil;
import com.stockasticappbackend.security.service.CustomUserDetailsService;
import com.stockasticappbackend.security.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for authentication endpoints.
 * Handles user login and JWT token generation.
 * All endpoints are publicly accessible.
 */
// Note: CORS handled by global CorsConfig - don't use @CrossOrigin("*") with credentials
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final CustomUserDetailsService userDetailsService;
        private final RefreshTokenService refreshTokenService;
        private final AppUserRepository appUserRepository;

        /**
         * Authenticates a user and returns a JWT token.
         *
         * @param request The login request containing email and password.
         * @return JwtResponse containing the token and user role.
         */
        @PostMapping("/login")
        public JwtResponse login(@RequestBody @Valid LoginRequest request, HttpServletResponse response, HttpServletRequest httpRequest) {
                AppUser existingUser = appUserRepository.findByEmail(request.getEmail()).orElse(null);
                if (existingUser != null && existingUser.getUserStatus() == UserStatus.BLOCKED) {
                        throw new IllegalStateException(ACCOUNT_BLOCKED_CONTACT_HELP);
                }

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.getEmail(),
                                                        request.getPassword()));
                } catch (DisabledException ex) {
                        throw new IllegalStateException(ACCOUNT_BLOCKED_CONTACT_HELP);
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

                String token = jwtUtil.generateToken(userDetails);

                String role = userDetails.getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority();
                
                AppUser user = appUserRepository.findByEmail(request.getEmail()).orElseThrow();
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

                // Set Refresh Token in HttpOnly Cookie
                ResponseCookie cookie = buildRefreshTokenCookie(refreshToken.getToken(), 24 * 60 * 60, httpRequest);

                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                return new JwtResponse(token, role, null); // Don't send refresh token in body
        }

        @PostMapping("/refreshtoken")
        public TokenRefreshResponse refreshtoken(
                @CookieValue(name = "refreshToken", required = false) String requestRefreshToken) {
            
            if (requestRefreshToken == null || requestRefreshToken.isEmpty()) {
                 throw new TokenRefreshException("null", REFRESH_TOKEN_MISSING_IN_COOKIE);
            }

            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        if (user.getUserStatus() != UserStatus.ACTIVE) {
                            refreshTokenService.deleteByToken(requestRefreshToken);
                            throw new TokenRefreshException(
                                    requestRefreshToken, USER_ACCOUNT_NOT_ACTIVE);
                        }
                         UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                        String token = jwtUtil.generateToken(userDetails);
                        // We don't rotate refresh token here for simplicity, but we could
                        return new TokenRefreshResponse(token, null);
                    })
                    .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                            REFRESH_TOKEN_NOT_IN_DATABASE));
        }

        @PostMapping("/logout")
        public ResponseEntity<?> logout(
                @CookieValue(name = "refreshToken", required = false) String refreshToken,
                HttpServletResponse response,
                HttpServletRequest httpRequest) {
            
            // Delete the refresh token from database if it exists
            if (refreshToken != null && !refreshToken.isEmpty()) {
                refreshTokenService.deleteByToken(refreshToken);
            }
            
            // Clear the cookie
            ResponseCookie cookie = buildRefreshTokenCookie("", 0, httpRequest);
            
             response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
             return ResponseEntity.ok(LOGOUT_SUCCESS);
        }

        private ResponseCookie buildRefreshTokenCookie(String tokenValue, long maxAgeSeconds, HttpServletRequest request) {
                boolean secureRequest = isSecureRequest(request);
                String sameSite = secureRequest ? "None" : "Lax";

                return ResponseCookie.from("refreshToken", tokenValue)
                        .httpOnly(true)
                        .secure(secureRequest)
                        .path("/")
                        // Keep host-only cookie so it works for local and tunnel hosts.
                        .maxAge(maxAgeSeconds)
                        .sameSite(sameSite)
                        .build();
        }

        private boolean isSecureRequest(HttpServletRequest request) {
                if (request.isSecure()) {
                        return true;
                }
                String forwardedProto = request.getHeader("X-Forwarded-Proto");
                return forwardedProto != null && "https".equalsIgnoreCase(forwardedProto);
        }
}
