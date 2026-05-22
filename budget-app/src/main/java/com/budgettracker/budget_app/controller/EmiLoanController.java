package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.requestdto.EmiLoanRequest;
import com.budgettracker.budget_app.responsedto.EmiLoanResponse;
import com.budgettracker.budget_app.service.EmiLoanService;
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
 * REST controller for managing EMI loan records for the authenticated user.
 */
@RestController
@RequestMapping("/emi")
@Slf4j
public class EmiLoanController {

    private final EmiLoanService service;

    @Autowired
    public EmiLoanController(@NonNull EmiLoanService service) {
        this.service = service;
    }

    /**
     * Returns all EMI loans belonging to the current user.
     */
    @GetMapping
    public ResponseEntity<List<EmiLoanResponse>> getAll() {
        String username = currentUsername();
        log.info("GET /emi - fetch all EMI loans for user: {}", username);
        List<EmiLoanResponse> loans = service.getAll();
        log.debug("GET /emi - returning {} EMI loan(s) for user: {}", loans.size(), username);
        return ResponseEntity.ok(loans);
    }

    /**
     * Creates a new EMI loan entry for the current user.
     */
    @PostMapping
    public ResponseEntity<EmiLoanResponse> create(@Valid @RequestBody EmiLoanRequest req) {
        String username = currentUsername();
        log.info("POST /emi - create EMI loan: loanName={}, principal={}, tenure={}m, user={}",
                req.getLoanName(), req.getPrincipal(), req.getTenureMonths(), username);
        EmiLoanResponse response = service.create(req);
        log.info("POST /emi - EMI loan created: id={}, loanName={}, user={}",
                response.getId(), response.getLoanName(), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Marks the next EMI instalment as paid for the given loan.
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<EmiLoanResponse> markPaid(@PathVariable Long id) {
        String username = currentUsername();
        log.info("POST /emi/{}/pay - mark EMI payment, user={}", id, username);
        EmiLoanResponse response = service.markPaid(id);
        log.info("POST /emi/{}/pay - payment recorded: paidMonths={}/{}, user={}",
                id, response.getPaidMonths(), response.getTenureMonths(), username);
        return ResponseEntity.ok(response);
    }

    /**
     * Reverses the last recorded EMI payment for the given loan.
     */
    @PostMapping("/{id}/undo-pay")
    public ResponseEntity<EmiLoanResponse> undoPay(@PathVariable Long id) {
        String username = currentUsername();
        log.info("POST /emi/{}/undo-pay - undo EMI payment, user={}", id, username);
        EmiLoanResponse response = service.undoPay(id);
        log.info("POST /emi/{}/undo-pay - payment undone: paidMonths={}/{}, user={}",
                id, response.getPaidMonths(), response.getTenureMonths(), username);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the specified EMI loan owned by the current user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String username = currentUsername();
        log.info("DELETE /emi/{} - delete EMI loan, user={}", id, username);
        service.delete(id);
        log.info("DELETE /emi/{} - EMI loan deleted, user={}", id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the username of the currently authenticated principal.
     */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}
