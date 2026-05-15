package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TransactionService transactionService;
    @MockBean JwtUtil jwtUtil;

    private TransactionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = TransactionResponse.builder()
                .id(1L)
                .type("INCOME")
                .amount(500.0)
                .category("Salary")
                .description("Monthly salary")
                .date(LocalDate.of(2024, 1, 15))
                .username("alice")
                .build();
    }

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void getAllTransactions_returnsList() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/transactions/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[0].amount").value(500.0));
    }

    @Test
    void getTransactions_paginated_returnsPage() throws Exception {
        when(transactionService.getTransactions(0, 10))
                .thenReturn(new PageImpl<>(List.of(sampleResponse)));

        mockMvc.perform(get("/transactions?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    @Test
    void addTransaction_validRequest_returns201() throws Exception {
        TransactionRequest req = buildRequest();
        when(transactionService.addTransaction(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void addTransaction_missingType_returns400() throws Exception {
        TransactionRequest req = buildRequest();
        req.setType(null);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addTransaction_invalidType_returns400() throws Exception {
        TransactionRequest req = buildRequest();
        req.setType("INVALID");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addTransaction_zeroAmount_returns400() throws Exception {
        TransactionRequest req = buildRequest();
        req.setAmount(0.0);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_validRequest_returns200() throws Exception {
        TransactionRequest req = buildRequest();
        when(transactionService.updateTransaction(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void updateTransaction_notFound_returns404() throws Exception {
        TransactionRequest req = buildRequest();
        when(transactionService.updateTransaction(any(), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Transaction not found with id: 99"));

        mockMvc.perform(put("/transactions/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransaction_forbidden_returns403() throws Exception {
        TransactionRequest req = buildRequest();
        when(transactionService.updateTransaction(any(), eq(1L)))
                .thenThrow(new ForbiddenException("You cannot update this transaction"));

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTransaction_returns204() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/transactions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Transaction not found with id: 99"))
                .when(transactionService).deleteTransaction(99L);

        mockMvc.perform(delete("/transactions/99"))
                .andExpect(status().isNotFound());
    }

    private TransactionRequest buildRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setType("INCOME");
        req.setAmount(500.0);
        req.setCategory("Salary");
        req.setDescription("Monthly salary");
        req.setDate(LocalDate.of(2024, 1, 15));
        return req;
    }
}
