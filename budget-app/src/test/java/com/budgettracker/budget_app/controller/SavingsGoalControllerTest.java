package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.requestdto.SavingsGoalRequest;
import com.budgettracker.budget_app.responsedto.SavingsGoalResponse;
import com.budgettracker.budget_app.service.SavingsGoalService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SavingsGoalController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class SavingsGoalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SavingsGoalService savingsGoalService;
    @MockBean com.budgettracker.budget_app.security.JwtUtil jwtUtil;

    private SavingsGoalResponse sampleResponse() {
        return SavingsGoalResponse.builder()
                .id(1L).title("Vacation Fund")
                .targetAmount(10000.0).savedAmount(2500.0)
                .deadline(LocalDate.now().plusYears(1)).build();
    }

    private SavingsGoalRequest validRequest() {
        return SavingsGoalRequest.builder()
                .title("Vacation Fund").targetAmount(10000.0)
                .savedAmount(2500.0).deadline(LocalDate.now().plusYears(1)).build();
    }

    // --- GET /savings-goals ---

    @Test
    @WithMockUser(username = "alice")
    void getAll_returns200WithList() throws Exception {
        when(savingsGoalService.getAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/savings-goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Vacation Fund"))
                .andExpect(jsonPath("$[0].targetAmount").value(10000.0));
    }

    @Test
    @WithMockUser(username = "alice")
    void getAll_whenEmpty_returns200EmptyArray() throws Exception {
        when(savingsGoalService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/savings-goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- POST /savings-goals ---

    @Test
    @WithMockUser(username = "alice")
    void create_validRequest_returns201() throws Exception {
        when(savingsGoalService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "alice")
    void create_missingTitle_returns400() throws Exception {
        SavingsGoalRequest req = validRequest();
        req.setTitle(null);

        mockMvc.perform(post("/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(savingsGoalService, never()).create(any());
    }

    @Test
    @WithMockUser(username = "alice")
    void create_negativeTarget_returns400() throws Exception {
        SavingsGoalRequest req = validRequest();
        req.setTargetAmount(0.5);

        mockMvc.perform(post("/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /savings-goals/{id} ---

    @Test
    @WithMockUser(username = "alice")
    void update_validRequest_returns200() throws Exception {
        SavingsGoalResponse updated = SavingsGoalResponse.builder()
                .id(1L).title("Emergency Fund").targetAmount(5000.0).savedAmount(1000.0).build();
        when(savingsGoalService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/savings-goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Emergency Fund"));
    }

    @Test
    @WithMockUser(username = "alice")
    void update_notFound_returns404() throws Exception {
        when(savingsGoalService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Savings goal not found"));

        mockMvc.perform(put("/savings-goals/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void update_forbidden_returns403() throws Exception {
        when(savingsGoalService.update(eq(1L), any()))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(put("/savings-goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /savings-goals/{id} ---

    @Test
    @WithMockUser(username = "alice")
    void delete_returns204() throws Exception {
        doNothing().when(savingsGoalService).delete(1L);

        mockMvc.perform(delete("/savings-goals/1"))
                .andExpect(status().isNoContent());

        verify(savingsGoalService).delete(1L);
    }

    @Test
    @WithMockUser(username = "alice")
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Savings goal not found"))
                .when(savingsGoalService).delete(99L);

        mockMvc.perform(delete("/savings-goals/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void delete_forbidden_returns403() throws Exception {
        doThrow(new ForbiddenException("Access denied")).when(savingsGoalService).delete(1L);

        mockMvc.perform(delete("/savings-goals/1"))
                .andExpect(status().isForbidden());
    }
}
