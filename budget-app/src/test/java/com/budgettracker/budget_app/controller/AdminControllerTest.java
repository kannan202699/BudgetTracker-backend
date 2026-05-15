package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.service.AdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AdminService adminService;
    @MockBean JwtUtil jwtUtil;

    @BeforeEach
    void setAdminAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_returnsList() throws Exception {
        UserResponse u = UserResponse.builder().id(1L).username("alice").role("USER").build();
        when(adminService.getAllUsers()).thenReturn(List.of(u));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void deleteUser_found_returns204() throws Exception {
        doNothing().when(adminService).deleteUser(1L);

        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(adminService).deleteUser(99L);

        mockMvc.perform(delete("/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_adminAccount_returns403() throws Exception {
        doThrow(new ForbiddenException("Admin accounts cannot be deleted"))
                .when(adminService).deleteUser(1L);

        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin accounts cannot be deleted"));
    }

    @Test
    void getAllTransactions_returnsList() throws Exception {
        TransactionResponse t = TransactionResponse.builder()
                .id(1L).type("EXPENSE").amount(200.0).category("Food")
                .date(LocalDate.now()).username("alice").build();
        when(adminService.getAllTransactions()).thenReturn(List.of(t));

        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    void deleteTransaction_found_returns204() throws Exception {
        doNothing().when(adminService).deleteTransaction(5L);

        mockMvc.perform(delete("/admin/transactions/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Transaction not found with id: 404"))
                .when(adminService).deleteTransaction(404L);

        mockMvc.perform(delete("/admin/transactions/404"))
                .andExpect(status().isNotFound());
    }
}
