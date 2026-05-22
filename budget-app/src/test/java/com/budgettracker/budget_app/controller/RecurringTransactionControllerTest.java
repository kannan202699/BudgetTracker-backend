package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.requestdto.RecurringTransactionRequest;
import com.budgettracker.budget_app.responsedto.RecurringTransactionResponse;
import com.budgettracker.budget_app.service.RecurringTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RecurringTransactionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class RecurringTransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean RecurringTransactionService recurringTransactionService;
    @MockBean com.budgettracker.budget_app.security.JwtUtil jwtUtil;

    private RecurringTransactionResponse sampleResponse() {
        return RecurringTransactionResponse.builder()
                .id(1L).type("EXPENSE").amount(500.0)
                .category("Groceries").frequency("MONTHLY").dayOfMonth(5).active(true).build();
    }

    private RecurringTransactionRequest validRequest() {
        return RecurringTransactionRequest.builder()
                .type("EXPENSE").amount(500.0).category("Groceries")
                .frequency("MONTHLY").dayOfMonth(5).active(true).build();
    }

    // --- GET /recurring ---

    @Test
    @WithMockUser(username = "alice")
    void getAll_returns200WithList() throws Exception {
        when(recurringTransactionService.getAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/recurring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Groceries"))
                .andExpect(jsonPath("$[0].frequency").value("MONTHLY"));
    }

    @Test
    @WithMockUser(username = "alice")
    void getAll_whenEmpty_returns200EmptyArray() throws Exception {
        when(recurringTransactionService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/recurring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- POST /recurring ---

    @Test
    @WithMockUser(username = "alice")
    void create_validRequest_returns201() throws Exception {
        when(recurringTransactionService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "alice")
    void create_invalidType_returns400() throws Exception {
        RecurringTransactionRequest req = validRequest();
        req.setType("INVALID");

        mockMvc.perform(post("/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(recurringTransactionService, never()).create(any());
    }

    @Test
    @WithMockUser(username = "alice")
    void create_missingCategory_returns400() throws Exception {
        RecurringTransactionRequest req = validRequest();
        req.setCategory(null);

        mockMvc.perform(post("/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /recurring/{id} ---

    @Test
    @WithMockUser(username = "alice")
    void update_validRequest_returns200() throws Exception {
        when(recurringTransactionService.update(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/recurring/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Groceries"));
    }

    @Test
    @WithMockUser(username = "alice")
    void update_notFound_returns404() throws Exception {
        when(recurringTransactionService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Recurring transaction not found"));

        mockMvc.perform(put("/recurring/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void update_forbidden_returns403() throws Exception {
        when(recurringTransactionService.update(eq(1L), any()))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(put("/recurring/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /recurring/{id} ---

    @Test
    @WithMockUser(username = "alice")
    void delete_returns204() throws Exception {
        doNothing().when(recurringTransactionService).delete(1L);

        mockMvc.perform(delete("/recurring/1"))
                .andExpect(status().isNoContent());

        verify(recurringTransactionService).delete(1L);
    }

    @Test
    @WithMockUser(username = "alice")
    void delete_forbidden_returns403() throws Exception {
        doThrow(new ForbiddenException("Access denied")).when(recurringTransactionService).delete(1L);

        mockMvc.perform(delete("/recurring/1"))
                .andExpect(status().isForbidden());
    }
}
