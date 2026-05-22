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

/**
 * Manages creation, validation, and revocation of refresh tokens.
 */
@Service
@Slf4j
public class RefreshTokenService {

    @Value("${refresh.token.validity.days:7}")
    private long validityDays;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Revokes any existing refresh token for the user and issues a fresh one.
     */
    @Transactional
    public RefreshToken createRefreshToken(UserRequest user) {
        log.debug("createRefreshToken - revoking existing token for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);

        Instant expiry = Instant.now().plus(validityDays, ChronoUnit.DAYS);
        log.debug("createRefreshToken - issuing new token for user: {}, expires: {}", user.getUsername(), expiry);

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(expiry)
                .build();

        RefreshToken saved = refreshTokenRepository.save(token);
        log.info("createRefreshToken - refresh token issued for user: {}", user.getUsername());
        return saved;
    }

    /**
     * Looks up a refresh token by its raw value; returns empty if not found.
     */
    public Optional<RefreshToken> findByToken(String token) {
        log.debug("findByToken - querying refresh token from repository");
        Optional<RefreshToken> result = refreshTokenRepository.findByToken(token);
        if (result.isEmpty()) {
            log.debug("findByToken - no matching refresh token found");
        } else {
            log.debug("findByToken - token found for user: {}", result.get().getUser().getUsername());
        }
        return result;
    }

    /**
     * Checks expiry; deletes the token and throws if it is expired.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        log.debug("verifyExpiration - checking expiry for user: {}, expiry: {}",
                token.getUser().getUsername(), token.getExpiryDate());

        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("verifyExpiration - refresh token expired for user: {}", token.getUser().getUsername());
            refreshTokenRepository.delete(token);
            throw new UnAuthorizedException("Session expired. Please log in again.");
        }

        log.debug("verifyExpiration - token is valid for user: {}", token.getUser().getUsername());
        return token;
    }

    /**
     * Deletes a refresh token by its raw value; used during logout and rotation.
     */
    @Transactional
    public void deleteByToken(String token) {
        log.debug("deleteByToken - deleting refresh token");
        refreshTokenRepository.deleteByToken(token);
        log.debug("deleteByToken - refresh token deleted");
    }

    /**
     * Deletes all refresh tokens belonging to the specified user.
     */
    @Transactional
    public void deleteByUser(UserRequest user) {
        log.debug("deleteByUser - deleting all refresh tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);
        log.debug("deleteByUser - refresh tokens deleted for user: {}", user.getUsername());
    }
}
