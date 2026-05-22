package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.requestdto.EmiLoanRequest;
import com.budgettracker.budget_app.responsedto.EmiLoanResponse;
import com.budgettracker.budget_app.service.EmiLoanService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EmiLoanController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class EmiLoanControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean EmiLoanService emiLoanService;
    @MockBean com.budgettracker.budget_app.security.JwtUtil jwtUtil;

    private EmiLoanResponse sampleResponse() {
        return EmiLoanResponse.builder()
                .id(1L).loanName("Home Loan").principal(100000.0)
                .interestRate(8.5).tenureMonths(12)
                .startDate(LocalDate.now().minusMonths(3))
                .emiAmount(8745.0).totalAmount(104940.0).totalInterest(4940.0)
                .paidMonths(2).monthsElapsed(3L).monthsRemaining(10L)
                .remainingBalance(85000.0).nextDueDate(LocalDate.now().plusMonths(1)).build();
    }

    // --- GET /emi ---

    @Test
    @WithMockUser(username = "alice")
    void getAll_returns200WithList() throws Exception {
        when(emiLoanService.getAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/emi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanName").value("Home Loan"))
                .andExpect(jsonPath("$[0].paidMonths").value(2));
    }

    @Test
    @WithMockUser(username = "alice")
    void getAll_whenEmpty_returns200EmptyArray() throws Exception {
        when(emiLoanService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/emi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- POST /emi ---

    @Test
    @WithMockUser(username = "alice")
    void create_validRequest_returns201() throws Exception {
        EmiLoanRequest req = EmiLoanRequest.builder()
                .loanName("Car Loan").principal(50000.0).interestRate(9.0)
                .tenureMonths(24).startDate(LocalDate.now()).build();
        when(emiLoanService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/emi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "alice")
    void create_missingLoanName_returns400() throws Exception {
        EmiLoanRequest req = EmiLoanRequest.builder()
                .principal(50000.0).interestRate(9.0)
                .tenureMonths(24).startDate(LocalDate.now()).build();

        mockMvc.perform(post("/emi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(emiLoanService, never()).create(any());
    }

    // --- POST /emi/{id}/pay ---

    @Test
    @WithMockUser(username = "alice")
    void markPaid_returns200() throws Exception {
        EmiLoanResponse updated = sampleResponse();
        when(emiLoanService.markPaid(1L)).thenReturn(updated);

        mockMvc.perform(post("/emi/1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidMonths").value(2));
    }

    @Test
    @WithMockUser(username = "alice")
    void markPaid_notFound_returns404() throws Exception {
        when(emiLoanService.markPaid(99L))
                .thenThrow(new ResourceNotFoundException("Loan not found"));

        mockMvc.perform(post("/emi/99/pay"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void markPaid_forbidden_returns403() throws Exception {
        when(emiLoanService.markPaid(1L))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(post("/emi/1/pay"))
                .andExpect(status().isForbidden());
    }

    // --- POST /emi/{id}/undo-pay ---

    @Test
    @WithMockUser(username = "alice")
    void undoPay_returns200() throws Exception {
        when(emiLoanService.undoPay(1L)).thenReturn(sampleResponse());

        mockMvc.perform(post("/emi/1/undo-pay"))
                .andExpect(status().isOk());

        verify(emiLoanService).undoPay(1L);
    }

    // --- DELETE /emi/{id} ---

    @Test
    @WithMockUser(username = "alice")
    void delete_returns204() throws Exception {
        doNothing().when(emiLoanService).delete(1L);

        mockMvc.perform(delete("/emi/1"))
                .andExpect(status().isNoContent());

        verify(emiLoanService).delete(1L);
    }

    @Test
    @WithMockUser(username = "alice")
    void delete_forbidden_returns403() throws Exception {
        doThrow(new ForbiddenException("Access denied")).when(emiLoanService).delete(1L);

        mockMvc.perform(delete("/emi/1"))
                .andExpect(status().isForbidden());
    }
}
