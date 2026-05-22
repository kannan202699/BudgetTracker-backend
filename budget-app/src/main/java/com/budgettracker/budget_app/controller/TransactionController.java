package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for CRUD operations on the authenticated user's transactions.
 */
@RestController
@RequestMapping("/transactions")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService service) {
        this.transactionService = service;
    }

    /**
     * Simple liveness probe; returns HTTP 200 with body "OK".
     */
    @Operation(summary = "Health check")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Returns all transactions for the current user without pagination.
     */
    @Operation(summary = "Get all transactions (no pagination)")
    @GetMapping("/all")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("GET /transactions/all - fetch all transactions");
        List<TransactionResponse> transactions = transactionService.getAllTransactions();
        log.debug("GET /transactions/all - returning {} transaction(s)", transactions.size());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Returns a paginated slice of the current user's transactions.
     */
    @Operation(summary = "Get paginated transactions")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Fetched successfully"))
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        log.info("GET /transactions - paginated fetch: page={}, size={}", page, size);
        Page<TransactionResponse> result = transactionService.getTransactions(page, size);
        log.debug("GET /transactions - returning {} transaction(s) on page {}", result.getNumberOfElements(), page);
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a new transaction for the current user and returns the saved record.
     */
    @Operation(summary = "Create a new transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> addTransaction(
            @Valid @RequestBody TransactionRequest transactionRequest) {
        log.info("POST /transactions - create: type={}, amount={}, category={}",
                transactionRequest.getType(), transactionRequest.getAmount(), transactionRequest.getCategory());
        TransactionResponse response = transactionService.addTransaction(transactionRequest);
        log.info("POST /transactions - transaction created: id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the specified transaction if it belongs to the current user.
     */
    @Operation(summary = "Update a transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest) {
        log.info("PUT /transactions/{} - update: type={}, amount={}, category={}",
                id, transactionRequest.getType(), transactionRequest.getAmount(), transactionRequest.getCategory());
        TransactionResponse response = transactionService.updateTransaction(transactionRequest, id);
        log.info("PUT /transactions/{} - transaction updated successfully", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the specified transaction if it belongs to the current user.
     */
    @Operation(summary = "Delete a transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        log.info("DELETE /transactions/{} - delete request", id);
        transactionService.deleteTransaction(id);
        log.info("DELETE /transactions/{} - transaction deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

}
