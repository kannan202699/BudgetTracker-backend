package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.requestdto.AuthRequest;
import com.budgettracker.budget_app.requestdto.RefreshToken;
import com.budgettracker.budget_app.requestdto.RefreshTokenRequest;
import com.budgettracker.budget_app.requestdto.RegisterRequest;
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
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Register a new user")
    @PostMapping("/register/user")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for user: {}", request.getUsername());
        authService.saveUser(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @Operation(summary = "Login and get JWT + refresh token")
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        AuthResponse response = authService.generateAuthToken(request);
        return ResponseEntity.ok(response);
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

        // Rotate: delete old refresh token, issue a new one
        refreshTokenService.deleteByToken(stored.getToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(stored.getUser());
        String newAccessToken = jwtUtil.generateToken(stored.getUser().getUsername(), stored.getUser().getRole());

        log.info("Token refreshed for user: {}", stored.getUser().getUsername());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
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
}
