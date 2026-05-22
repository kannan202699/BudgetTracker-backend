package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.requestdto.AuthRequest;
import com.budgettracker.budget_app.requestdto.RefreshToken;
import com.budgettracker.budget_app.requestdto.RefreshTokenRequest;
import com.budgettracker.budget_app.requestdto.RegisterRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.AuthResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.service.AuthService;
import com.budgettracker.budget_app.service.RefreshTokenService;
import com.budgettracker.budget_app.util.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtUtil jwtUtil;
    @MockBean RefreshTokenService refreshTokenService;

    @Test
    void register_validRequest_returns200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("Valid@Pass1");
        req.setEmail("newuser@example.com");
        req.setVerifiedToken("some-verified-token");
        doNothing().when(authService).saveUser(any());

        mockMvc.perform(post("/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("");
        req.setPassword("Valid@Pass1");

        mockMvc.perform(post("/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("");

        mockMvc.perform(post("/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_usernameTaken_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("taken");
        req.setPassword("Valid@Pass1");
        req.setEmail("taken@example.com");
        req.setVerifiedToken("some-verified-token");
        doThrow(new RuntimeException("Username is already taken")).when(authService).saveUser(any());

        mockMvc.perform(post("/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        AuthRequest req = AuthRequest.builder().username("alice").password("pass").build();
        AuthResponse resp = AuthResponse.builder()
                .token("jwt-token").role("USER").username("alice").userId(1L).build();
        when(authService.generateAuthToken(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        AuthRequest req = AuthRequest.builder().username("alice").password("wrong").build();
        when(authService.generateAuthToken(any())).thenThrow(new UnAuthorizedException("Invalid credentials"));

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        AuthRequest req = AuthRequest.builder().username("").password("pass").build();

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validToken_returnsNewTokens() throws Exception {
        UserRequest user = new UserRequest();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(Role.USER);

        RefreshToken storedToken = RefreshToken.builder()
                .token("old-refresh-token")
                .user(user)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .token("new-refresh-token")
                .user(user)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokenService.findByToken("old-refresh-token")).thenReturn(java.util.Optional.of(storedToken));
        when(refreshTokenService.verifyExpiration(storedToken)).thenReturn(storedToken);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(newRefreshToken);
        when(jwtUtil.generateToken("alice", Role.USER)).thenReturn("new-access-token");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old-refresh-token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.username").value("alice"));

        verify(refreshTokenService).deleteByToken("old-refresh-token");
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(refreshTokenService.findByToken("bad-token")).thenReturn(java.util.Optional.empty());

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("bad-token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_blankToken_returns401() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("  ");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withValidToken_returns200() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("some-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(refreshTokenService).deleteByToken("some-refresh-token");
    }

    @Test
    void logout_withNullToken_returns200WithoutCallingDelete() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(null);

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(refreshTokenService, never()).deleteByToken(anyString());
    }
}
