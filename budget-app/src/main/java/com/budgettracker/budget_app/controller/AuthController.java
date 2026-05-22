package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.requestdto.*;
import com.budgettracker.budget_app.responsedto.AuthResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.service.AuthService;
import com.budgettracker.budget_app.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(@NonNull AuthService authService,
                          @NonNull RefreshTokenService refreshTokenService,
                          @NonNull JwtUtil jwtUtil) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    // ── Email Verification ──────────────────────────────────────────────────

    @Operation(summary = "Send 6-digit OTP to the given email for registration verification")
    @PostMapping("/verify-email/send")
    public ResponseEntity<Map<String, String>> sendVerification(
            @Valid @RequestBody SendVerificationRequest request) {
        log.info("POST /auth/verify-email/send - {}", request.getEmail());
        authService.sendEmailVerification(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verification code sent to " + request.getEmail()));
    }

    @Operation(summary = "Confirm OTP — returns a verifiedToken required for registration")
    @PostMapping("/verify-email/confirm")
    public ResponseEntity<Map<String, String>> confirmVerification(
            @Valid @RequestBody VerifyEmailRequest request) {
        log.info("POST /auth/verify-email/confirm - {}", request.getEmail());
        String verifiedToken = authService.verifyEmailOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("verifiedToken", verifiedToken));
    }

    // ── Registration ────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user (requires verifiedToken from email verification)")
    @PostMapping("/register/user")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register/user - username: {}", request.getUsername());
        authService.saveUser(request);
        return ResponseEntity.ok("User registered successfully");
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Operation(summary = "Login with username or email — returns JWT + refresh token")
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("POST /auth/token - {}", request.getUsername());
        return ResponseEntity.ok(authService.generateAuthToken(request));
    }

    @Operation(summary = "Exchange a valid refresh token for a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new UnAuthorizedException("Refresh token is required");
        }
        RefreshToken stored = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnAuthorizedException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(stored);
        refreshTokenService.deleteByToken(stored.getToken());
        RefreshToken newRT = refreshTokenService.createRefreshToken(stored.getUser());
        String newAt = jwtUtil.generateToken(stored.getUser().getUsername(), stored.getUser().getRole());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(newAt)
                .refreshToken(newRT.getToken())
                .role(stored.getUser().getRole().name())
                .username(stored.getUser().getUsername())
                .userId(stored.getUser().getId())
                .build());
    }

    @Operation(summary = "Logout and invalidate the refresh token")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenService.deleteByToken(request.getRefreshToken());
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    // ── Forgot / Reset Password ──────────────────────────────────────────────

    @Operation(summary = "Request a password reset link via email")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "If that email is registered, you will receive a reset link shortly."));
    }

    @Operation(summary = "Reset password using a valid reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please sign in."));
    }

}
