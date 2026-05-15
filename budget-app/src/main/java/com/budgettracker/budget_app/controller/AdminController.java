package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Get all registered users")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Admin: fetch all users");
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @Operation(summary = "Delete a user and all their transactions")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Admin: delete user id={}", id);
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all transactions across all users")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("Admin: fetch all transactions");
        return ResponseEntity.ok(adminService.getAllTransactions());
    }

    @Operation(summary = "Delete any transaction")
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        log.info("Admin: delete transaction id={}", id);
        adminService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
