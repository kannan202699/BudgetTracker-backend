package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.requestdto.SavingsGoalRequest;
import com.budgettracker.budget_app.responsedto.SavingsGoalResponse;
import com.budgettracker.budget_app.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing savings goals for the authenticated user.
 */
@RestController
@RequestMapping("/savings-goals")
@Slf4j
public class SavingsGoalController {

    private final SavingsGoalService service;

    @Autowired
    public SavingsGoalController(@NonNull SavingsGoalService service) {
        this.service = service;
    }

    /**
     * Returns all savings goals belonging to the current user.
     */
    @GetMapping
    public ResponseEntity<List<SavingsGoalResponse>> getAll() {
        String username = currentUsername();
        log.info("GET /savings-goals - fetch all savings goals for user: {}", username);
        List<SavingsGoalResponse> goals = service.getAll();
        log.debug("GET /savings-goals - returning {} savings goal(s) for user: {}", goals.size(), username);
        return ResponseEntity.ok(goals);
    }

    /**
     * Creates a new savings goal for the current user.
     */
    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest req) {
        String username = currentUsername();
        log.info("POST /savings-goals - create: title={}, target={}, user={}",
                req.getTitle(), req.getTargetAmount(), username);
        SavingsGoalResponse response = service.create(req);
        log.info("POST /savings-goals - savings goal created: id={}, title={}, user={}",
                response.getId(), response.getTitle(), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing savings goal owned by the current user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> update(
            @PathVariable Long id, @Valid @RequestBody SavingsGoalRequest req) {
        String username = currentUsername();
        log.info("PUT /savings-goals/{} - update: title={}, target={}, user={}",
                id, req.getTitle(), req.getTargetAmount(), username);
        SavingsGoalResponse response = service.update(id, req);
        log.info("PUT /savings-goals/{} - savings goal updated, user={}", id, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the specified savings goal owned by the current user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String username = currentUsername();
        log.info("DELETE /savings-goals/{} - delete request, user={}", id, username);
        service.delete(id);
        log.info("DELETE /savings-goals/{} - savings goal deleted, user={}", id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the username of the currently authenticated principal.
     */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
