package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.repository.RefreshTokenRepository;
import com.budgettracker.budget_app.requestdto.RefreshToken;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UserRequest user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "validityDays", 7L);

        user = new UserRequest();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(Role.USER);
    }

    @Test
    void createRefreshToken_deletesOldAndCreatesNew() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        verify(refreshTokenRepository).deleteByUser(user);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiryDate()).isAfter(Instant.now());
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createRefreshToken_expiryIsSevenDaysFromNow() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        Instant sevenDaysFromNow = Instant.now().plus(7, ChronoUnit.DAYS);
        assertThat(result.getExpiryDate()).isBefore(sevenDaysFromNow.plusSeconds(5));
        assertThat(result.getExpiryDate()).isAfter(sevenDaysFromNow.minusSeconds(5));
    }

    @Test
    void findByToken_delegatesToRepository() {
        RefreshToken token = RefreshToken.builder().token("uuid-123").user(user)
                .expiryDate(Instant.now().plusSeconds(3600)).build();
        when(refreshTokenRepository.findByToken("uuid-123")).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = refreshTokenService.findByToken("uuid-123");

        assertThat(result).isPresent().contains(token);
    }

    @Test
    void findByToken_whenNotFound_returnsEmpty() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void verifyExpiration_whenTokenValid_returnsToken() {
        RefreshToken token = RefreshToken.builder().token("uuid-abc").user(user)
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS)).build();

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isEqualTo(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_whenTokenExpired_deletesAndThrows() {
        RefreshToken token = RefreshToken.builder().token("expired-token").user(user)
                .expiryDate(Instant.now().minus(1, ChronoUnit.MINUTES)).build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void deleteByToken_delegatesToRepository() {
        refreshTokenService.deleteByToken("uuid-del");

        verify(refreshTokenRepository).deleteByToken("uuid-del");
    }

    @Test
    void deleteByUser_delegatesToRepository() {
        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}
