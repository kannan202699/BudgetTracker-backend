package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.requestdto.RecurringTransactionRequest;
import com.budgettracker.budget_app.responsedto.RecurringTransactionResponse;
import com.budgettracker.budget_app.service.RecurringTransactionService;
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
 * REST controller for managing recurring transactions for the authenticated user.
 */
@RestController
@RequestMapping("/recurring")
@Slf4j
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @Autowired
    public RecurringTransactionController(@NonNull RecurringTransactionService service) {
        this.service = service;
    }

    /**
     * Returns all recurring transactions belonging to the current user.
     */
    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> getAll() {
        String username = currentUsername();
        log.info("GET /recurring - fetch all recurring transactions for user: {}", username);
        List<RecurringTransactionResponse> list = service.getAll();
        log.debug("GET /recurring - returning {} recurring transaction(s) for user: {}", list.size(), username);
        return ResponseEntity.ok(list);
    }

    /**
     * Creates a new recurring transaction for the current user.
     */
    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(@Valid @RequestBody RecurringTransactionRequest req) {
        String username = currentUsername();
        log.info("POST /recurring - create: type={}, amount={}, category={}, frequency={}, user={}",
                req.getType(), req.getAmount(), req.getCategory(), req.getFrequency(), username);
        RecurringTransactionResponse response = service.create(req);
        log.info("POST /recurring - recurring transaction created: id={}, user={}", response.getId(), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing recurring transaction owned by the current user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> update(
            @PathVariable Long id, @Valid @RequestBody RecurringTransactionRequest req) {
        String username = currentUsername();
        log.info("PUT /recurring/{} - update: type={}, amount={}, category={}, user={}",
                id, req.getType(), req.getAmount(), req.getCategory(), username);
        RecurringTransactionResponse response = service.update(id, req);
        log.info("PUT /recurring/{} - recurring transaction updated, user={}", id, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the specified recurring transaction owned by the current user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String username = currentUsername();
        log.info("DELETE /recurring/{} - delete request, user={}", id, username);
        service.delete(id);
        log.info("DELETE /recurring/{} - recurring transaction deleted, user={}", id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the username of the currently authenticated principal.
     */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}
