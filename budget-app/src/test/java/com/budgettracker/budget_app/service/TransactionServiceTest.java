package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.TransactionRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UserRequest userAlice;
    private UserRequest userBob;

    @BeforeEach
    void setUpUser() {
        userAlice = new UserRequest();
        userAlice.setId(1L);
        userAlice.setUsername("alice");
        userAlice.setRole(Role.USER);

        userBob = new UserRequest();
        userBob.setId(2L);
        userBob.setUsername("bob");
        userBob.setRole(Role.USER);

        setAuth("alice");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    private TransactionRequest buildTxn(Long id, UserRequest owner) {
        TransactionRequest t = new TransactionRequest();
        t.setId(id);
        t.setType("INCOME");
        t.setAmount(100.0);
        t.setCategory("Salary");
        t.setDate(LocalDate.now());
        t.setUser(owner);
        return t;
    }

    // --- addTransaction ---

    @Test
    void addTransaction_savesAndReturnsResponse() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest req = buildTxn(null, null);

        TransactionResponse resp = transactionService.addTransaction(req);

        verify(transactionRepository).save(req);
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getType()).isEqualTo("INCOME");
    }

    // --- getAllTransactions ---

    @Test
    void getAllTransactions_regularUser_returnsOwnTransactions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest t = buildTxn(1L, userAlice);
        when(transactionRepository.findByUser(eq(userAlice), any(Sort.class)))
                .thenReturn(List.of(t));

        List<TransactionResponse> results = transactionService.getAllTransactions();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void getAllTransactions_adminUser_returnsAllTransactions() {
        UserRequest admin = new UserRequest();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        setAuth("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        TransactionRequest t1 = buildTxn(1L, userAlice);
        TransactionRequest t2 = buildTxn(2L, userBob);
        when(transactionRepository.findAllByOrderByDateDesc()).thenReturn(List.of(t1, t2));

        List<TransactionResponse> results = transactionService.getAllTransactions();

        assertThat(results).hasSize(2);
    }

    // --- getTransactions (paginated) ---

    @Test
    void getTransactions_userRole_returnsPaginatedOwnTransactions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest t = buildTxn(1L, userAlice);
        Page<TransactionRequest> page = new PageImpl<>(List.of(t));
        when(transactionRepository.findByUser(eq(userAlice), any(Pageable.class))).thenReturn(page);

        Page<TransactionResponse> result = transactionService.getTransactions(0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getTransactions_adminRole_returnsPaginatedAllTransactions() {
        UserRequest admin = new UserRequest();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        setAuth("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        Page<TransactionRequest> page = new PageImpl<>(List.of(buildTxn(1L, userAlice), buildTxn(2L, userBob)));
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<TransactionResponse> result = transactionService.getTransactions(0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // --- updateTransaction ---

    @Test
    void updateTransaction_ownTransaction_updatesAndReturns() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest existing = buildTxn(1L, userAlice);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        TransactionRequest update = buildTxn(null, null);
        update.setAmount(200.0);
        update.setCategory("Food");

        TransactionResponse resp = transactionService.updateTransaction(update, 1L);

        assertThat(resp.getAmount()).isEqualTo(200.0);
        assertThat(resp.getCategory()).isEqualTo("Food");
        verify(transactionRepository).save(existing);
    }

    @Test
    void updateTransaction_otherUserTransaction_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest existing = buildTxn(1L, userBob);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.updateTransaction(buildTxn(null, null), 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateTransaction_notFound_throwsResourceNotFound() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(buildTxn(null, null), 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- deleteTransaction ---

    @Test
    void deleteTransaction_ownTransaction_deletes() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest t = buildTxn(1L, userAlice);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).deleteById(1L);
    }

    @Test
    void deleteTransaction_otherUserTransaction_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        TransactionRequest t = buildTxn(1L, userBob);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> transactionService.deleteTransaction(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteTransaction_notFound_throwsResourceNotFound() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTransaction_adminCanDeleteAny() {
        UserRequest admin = new UserRequest();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        setAuth("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        TransactionRequest t = buildTxn(1L, userAlice);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).deleteById(1L);
    }

    // --- getCurrentUser ---

    @Test
    void getCurrentUser_returnsUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(userAlice));
        assertThat(transactionService.getCurrentUser().getUsername()).isEqualTo("alice");
    }
}
