package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.repository.EmailVerificationTokenRepository;
import com.budgettracker.budget_app.repository.PasswordResetTokenRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.AuthRequest;
import com.budgettracker.budget_app.requestdto.EmailVerificationToken;
import com.budgettracker.budget_app.requestdto.RefreshToken;
import com.budgettracker.budget_app.requestdto.RegisterRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.AuthResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RefreshToken fakeRefreshToken(UserRequest user) {
        return RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(86400))
                .build();
    }

    private EmailVerificationToken verifiedEvToken(String email, String token) {
        return EmailVerificationToken.builder()
                .email(email)
                .otp("123456")
                .verified(true)
                .verifiedToken(token)
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusSeconds(1800))
                .build();
    }

    // --- saveUser ---

    @Test
    void saveUser_newUser_savesSuccessfully() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("Valid@Pass1");
        req.setEmail("alice@example.com");
        req.setVerifiedToken("verified-token");

        when(emailVerificationTokenRepository.findByVerifiedToken("verified-token"))
                .thenReturn(Optional.of(verifiedEvToken("alice@example.com", "verified-token")));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Valid@Pass1")).thenReturn("hashed");

        authService.saveUser(req);

        ArgumentCaptor<UserRequest> captor = ArgumentCaptor.forClass(UserRequest.class);
        verify(userRepository).save(captor.capture());
        UserRequest saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void saveUser_usernameTaken_throwsRuntimeException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("Valid@Pass1");
        req.setEmail("alice@example.com");
        req.setVerifiedToken("verified-token");

        UserRequest existing = new UserRequest();
        existing.setUsername("alice");
        when(emailVerificationTokenRepository.findByVerifiedToken("verified-token"))
                .thenReturn(Optional.of(verifiedEvToken("alice@example.com", "verified-token")));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.saveUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveUser_trimmedUsername_saved() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("  bob  ");
        req.setPassword("Valid@Pass1");
        req.setEmail("bob@example.com");
        req.setVerifiedToken("verified-token");

        when(emailVerificationTokenRepository.findByVerifiedToken("verified-token"))
                .thenReturn(Optional.of(verifiedEvToken("bob@example.com", "verified-token")));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        authService.saveUser(req);

        ArgumentCaptor<UserRequest> captor = ArgumentCaptor.forClass(UserRequest.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("bob");
    }

    // --- generateAuthToken ---

    @Test
    void generateAuthToken_validCredentials_returnsAuthResponse() {
        AuthRequest req = AuthRequest.builder().username("alice").password("Valid@Pass1").build();
        UserRequest user = new UserRequest();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("hashed");
        user.setRole(Role.USER);

        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Valid@Pass1", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("alice", Role.USER)).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(fakeRefreshToken(user));

        AuthResponse response = authService.generateAuthToken(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-token");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void generateAuthToken_userNotFound_throwsUnauthorized() {
        AuthRequest req = AuthRequest.builder().username("unknown").password("pass").build();
        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.generateAuthToken(req))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void generateAuthToken_wrongPassword_throwsUnauthorized() {
        AuthRequest req = AuthRequest.builder().username("alice").password("wrong").build();
        UserRequest user = new UserRequest();
        user.setUsername("alice");
        user.setPassword("hashed");
        user.setRole(Role.USER);

        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.generateAuthToken(req))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void generateAuthToken_adminRole_returnsAdminResponse() {
        AuthRequest req = AuthRequest.builder().username("admin").password("Admin@1!").build();
        UserRequest user = new UserRequest();
        user.setId(99L);
        user.setUsername("admin");
        user.setPassword("hashed-admin");
        user.setRole(Role.ADMIN);

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@1!", "hashed-admin")).thenReturn(true);
        when(jwtUtil.generateToken("admin", Role.ADMIN)).thenReturn("admin-jwt");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(fakeRefreshToken(user));

        AuthResponse response = authService.generateAuthToken(req);

        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getToken()).isEqualTo("admin-jwt");
        assertThat(response.getRefreshToken()).isNotBlank();
    }
}
