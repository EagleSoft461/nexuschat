package com.nexuschat.service;

import com.nexuschat.model.RefreshToken;
import com.nexuschat.model.User;
import com.nexuschat.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // ms

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for this user (single active session per user)
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpiration))
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken verifyAndGet(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new IllegalArgumentException("Refresh token has expired, please log in again");
        }

        return token;
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}
