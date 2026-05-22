package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing admin-only operations: view/delete any user or transaction.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(@NonNull AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Returns a list of all registered users in the system.
     */
    @Operation(summary = "Get all registered users")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /admin/users - fetch all users");
        List<UserResponse> users = adminService.getAllUsers();
        log.debug("GET /admin/users - returning {} user(s)", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * Deletes the specified user along with all their associated data.
     */
    @Operation(summary = "Delete a user and all their transactions")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /admin/users/{} - delete user request", id);
        adminService.deleteUser(id);
        log.info("DELETE /admin/users/{} - user deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns all transactions across every user, ordered by date descending.
     */
    @Operation(summary = "Get all transactions across all users")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("GET /admin/transactions - fetch all transactions");
        List<TransactionResponse> transactions = adminService.getAllTransactions();
        log.debug("GET /admin/transactions - returning {} transaction(s)", transactions.size());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Deletes the specified transaction regardless of which user owns it.
     */
    @Operation(summary = "Delete any transaction")
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        log.info("DELETE /admin/transactions/{} - delete transaction request", id);
        adminService.deleteTransaction(id);
        log.info("DELETE /admin/transactions/{} - transaction deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

}
