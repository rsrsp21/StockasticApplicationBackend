package com.stockasticappbackend.security.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.exception.TokenRefreshException;
import com.stockasticappbackend.model.entity.RefreshToken;
import com.stockasticappbackend.repository.RefreshTokenRepository;
import com.stockasticappbackend.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    // Maximum number of active sessions (devices) per user
    private static final int MAX_TOKENS_PER_USER = 3;

    @Value("${jwt.refresh.expiration:86400000}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Check if user has exceeded max tokens and remove oldest ones
        int currentTokenCount = refreshTokenRepository.countByUserId(userId);
        if (currentTokenCount >= MAX_TOKENS_PER_USER) {
            // Get all tokens ordered by expiry date (oldest first)
            List<RefreshToken> tokens = refreshTokenRepository.findByUserIdOrderByExpiryDateAsc(userId);
            // Delete oldest tokens until we're under the limit
            int tokensToDelete = currentTokenCount - MAX_TOKENS_PER_USER + 1; // +1 for the new token
            for (int i = 0; i < tokensToDelete && i < tokens.size(); i++) {
                refreshTokenRepository.delete(tokens.get(i));
            }
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).get());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }

    // Delete a specific token (for logout)
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}
