package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.requestdto.SetGoalRequest;
import com.budgettracker.budget_app.service.BudgetGoalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing per-category budget goals for the authenticated user.
 */
@RestController
@RequestMapping("/budget")
@Slf4j
public class BudgetGoalController {

    private final BudgetGoalService budgetGoalService;

    @Autowired
    public BudgetGoalController(@NonNull BudgetGoalService budgetGoalService) {
        this.budgetGoalService = budgetGoalService;
    }

    /**
     * Returns all budget goals for the current user keyed by category.
     */
    @Operation(summary = "Get all budget goals for the current user")
    @GetMapping("/goals")
    public ResponseEntity<Map<String, Double>> getGoals() {
        String username = currentUsername();
        log.info("GET /budget/goals - fetch budget goals for user: {}", username);
        Map<String, Double> goals = budgetGoalService.getGoals();
        log.debug("GET /budget/goals - returning {} goal(s) for user: {}", goals.size(), username);
        return ResponseEntity.ok(goals);
    }

    /**
     * Creates or updates the budget goal for the specified category.
     */
    @Operation(summary = "Set or update the budget goal for a category")
    @PutMapping("/goals/{category}")
    public ResponseEntity<Void> setGoal(@PathVariable String category,
                                        @Valid @RequestBody SetGoalRequest request) {
        String username = currentUsername();
        log.info("PUT /budget/goals/{} - set goal: amount={}, user={}", category, request.getAmount(), username);
        budgetGoalService.setGoal(category, request.getAmount());
        log.info("PUT /budget/goals/{} - budget goal saved, user={}", category, username);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes the budget goal for the specified category.
     */
    @Operation(summary = "Remove the budget goal for a category")
    @DeleteMapping("/goals/{category}")
    public ResponseEntity<Void> deleteGoal(@PathVariable String category) {
        String username = currentUsername();
        log.info("DELETE /budget/goals/{} - delete goal, user={}", category, username);
        budgetGoalService.deleteGoal(category);
        log.info("DELETE /budget/goals/{} - budget goal removed, user={}", category, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the username of the currently authenticated principal.
     */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}
