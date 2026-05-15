package com.budgettracker.budget_app.exception;

import com.budgettracker.budget_app.controller.AuthController;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.service.AuthService;
import com.budgettracker.budget_app.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtUtil jwtUtil;
    @MockBean RefreshTokenService refreshTokenService;

    private static final String LOGIN_URL = "/auth/token";
    private static final String LOGIN_BODY = "{\"username\":\"u\",\"password\":\"p\"}";

    @Test
    void unauthorizedException_returns401() throws Exception {
        when(authService.generateAuthToken(any()))
                .thenThrow(new UnAuthorizedException("Invalid credentials"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void runtimeException_returns400() throws Exception {
        when(authService.generateAuthToken(any()))
                .thenThrow(new RuntimeException("Username is already taken"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void accessDeniedException_returns403() throws Exception {
        when(authService.generateAuthToken(any()))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void validationException_returns400WithFieldErrors() throws Exception {
        // Send empty body — triggers @NotBlank on username/password
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void forbiddenException_returns403() throws Exception {
        when(authService.generateAuthToken(any()))
                .thenThrow(new ForbiddenException("Forbidden action"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden action"));
    }

    @Test
    void resourceNotFoundException_returns404() throws Exception {
        when(authService.generateAuthToken(any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
