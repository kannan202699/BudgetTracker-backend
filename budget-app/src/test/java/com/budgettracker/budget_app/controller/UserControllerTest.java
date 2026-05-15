package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.requestdto.ProfileUpdateRequest;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.util.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;

    private UserRequest alice;

    @BeforeEach
    void setUpUser() {
        alice = new UserRequest();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setRole(Role.USER);
        alice.setEmail("alice@example.com");
        alice.setFullName("Alice Smith");
        alice.setPhone("9876543210");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProfile_returnsUserResponse() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"));
    }

    @Test
    void getProfile_userNotFound_returns404() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_validRequest_updatesAndReturns() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.save(any())).thenReturn(alice);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setEmail("newemail@example.com");
        req.setFullName("Alice Updated");
        req.setPhone("1234567890");

        mockMvc.perform(put("/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setEmail("not-an-email");

        mockMvc.perform(put("/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_clearEmail_accepts() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.save(any())).thenReturn(alice);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setEmail("");

        mockMvc.perform(put("/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
