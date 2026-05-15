package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.requestdto.SetGoalRequest;
import com.budgettracker.budget_app.service.BudgetGoalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/budget")
@Slf4j
public class BudgetGoalController {

    private final BudgetGoalService budgetGoalService;

    public BudgetGoalController(BudgetGoalService budgetGoalService) {
        this.budgetGoalService = budgetGoalService;
    }

    @Operation(summary = "Get all budget goals for the current user")
    @GetMapping("/goals")
    public ResponseEntity<Map<String, Double>> getGoals() {
        return ResponseEntity.ok(budgetGoalService.getGoals());
    }

    @Operation(summary = "Set or update the budget goal for a category")
    @PutMapping("/goals/{category}")
    public ResponseEntity<Void> setGoal(@PathVariable String category,
                                        @Valid @RequestBody SetGoalRequest request) {
        budgetGoalService.setGoal(category, request.getAmount());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove the budget goal for a category")
    @DeleteMapping("/goals/{category}")
    public ResponseEntity<Void> deleteGoal(@PathVariable String category) {
        budgetGoalService.deleteGoal(category);
        return ResponseEntity.noContent().build();
    }

}
