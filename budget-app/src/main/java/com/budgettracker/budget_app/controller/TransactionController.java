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

@RestController
@RequestMapping("/transactions")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService service) {
        this.transactionService = service;
    }

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @Operation(summary = "Get all transactions (no pagination)")
    @GetMapping("/all")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("Fetch all transactions request");
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @Operation(summary = "Get paginated transactions")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Fetched successfully"))
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        log.info("Fetch paginated transactions: page={}, size={}", page, size);
        return ResponseEntity.ok(transactionService.getTransactions(page, size));
    }

    @Operation(summary = "Create a new transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> addTransaction(
            @Valid @RequestBody TransactionRequest transactionRequest) {
        log.info("Create transaction request received");
        TransactionResponse response = transactionService.addTransaction(transactionRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest) {
        log.info("Update transaction request for id: {}", id);
        return ResponseEntity.ok(transactionService.updateTransaction(transactionRequest, id));
    }

    @Operation(summary = "Delete a transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        log.info("Delete transaction request for id: {}", id);
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
