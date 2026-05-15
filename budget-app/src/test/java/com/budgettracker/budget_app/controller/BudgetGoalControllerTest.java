package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.GlobalExceptionHandler;
import com.budgettracker.budget_app.requestdto.SetGoalRequest;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.service.BudgetGoalService;
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

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = BudgetGoalController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(GlobalExceptionHandler.class)
class BudgetGoalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BudgetGoalService budgetGoalService;
    @MockBean JwtUtil jwtUtil;

    @Test
    void getGoals_returnsMapFromService() throws Exception {
        when(budgetGoalService.getGoals()).thenReturn(Map.of("Food", 3000.0, "Transport", 1500.0));

        mockMvc.perform(get("/budget/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Food").value(3000.0))
                .andExpect(jsonPath("$.Transport").value(1500.0));
    }

    @Test
    void getGoals_whenEmpty_returnsEmptyMap() throws Exception {
        when(budgetGoalService.getGoals()).thenReturn(Map.of());

        mockMvc.perform(get("/budget/goals"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void setGoal_validRequest_returns200() throws Exception {
        SetGoalRequest request = new SetGoalRequest();
        request.setAmount(2500.0);

        mockMvc.perform(put("/budget/goals/Food")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(budgetGoalService).setGoal("Food", 2500.0);
    }

    @Test
    void setGoal_amountBelowMinimum_returns400() throws Exception {
        SetGoalRequest request = new SetGoalRequest();
        request.setAmount(0.5);

        mockMvc.perform(put("/budget/goals/Food")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(budgetGoalService, never()).setGoal(any(), anyDouble());
    }

    @Test
    void setGoal_nullAmount_returns400() throws Exception {
        mockMvc.perform(put("/budget/goals/Food")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": null}"))
                .andExpect(status().isBadRequest());

        verify(budgetGoalService, never()).setGoal(any(), anyDouble());
    }

    @Test
    void setGoal_amountExceedsMaximum_returns400() throws Exception {
        SetGoalRequest request = new SetGoalRequest();
        request.setAmount(100_000_000.0);

        mockMvc.perform(put("/budget/goals/Transport")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(budgetGoalService, never()).setGoal(any(), anyDouble());
    }

    @Test
    void deleteGoal_returns204() throws Exception {
        mockMvc.perform(delete("/budget/goals/Food"))
                .andExpect(status().isNoContent());

        verify(budgetGoalService).deleteGoal("Food");
    }

    @Test
    void deleteGoal_urlEncodedCategory_passesDecodedToService() throws Exception {
        mockMvc.perform(delete("/budget/goals/Entertainment"))
                .andExpect(status().isNoContent());

        verify(budgetGoalService).deleteGoal("Entertainment");
    }
}
