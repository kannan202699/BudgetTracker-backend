package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.repository.RefreshTokenRepository;
import com.budgettracker.budget_app.requestdto.RefreshToken;
import com.budgettracker.budget_app.requestdto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {

    @Value("${refresh.token.validity.days:7}")
    private long validityDays;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(UserRequest user) {
        // One active refresh token per user — revoke any existing one
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plus(validityDays, ChronoUnit.DAYS))
                .build();

        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired for user: {}", token.getUser().getUsername());
            throw new UnAuthorizedException("Session expired. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteByUser(UserRequest user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
