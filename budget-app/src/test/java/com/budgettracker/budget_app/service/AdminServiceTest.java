package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.BudgetGoalRepository;
import com.budgettracker.budget_app.repository.EmiLoanRepository;
import com.budgettracker.budget_app.repository.PasswordResetTokenRepository;
import com.budgettracker.budget_app.repository.RecurringTransactionRepository;
import com.budgettracker.budget_app.repository.RefreshTokenRepository;
import com.budgettracker.budget_app.repository.SavingsGoalRepository;
import com.budgettracker.budget_app.repository.TransactionRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.util.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BudgetGoalRepository budgetGoalRepository;
    @Mock private EmiLoanRepository emiLoanRepository;
    @Mock private RecurringTransactionRepository recurringTransactionRepository;
    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private AdminService adminService;

    private UserRequest regularUser;
    private UserRequest adminUser;

    @BeforeEach
    void setUp() {
        regularUser = new UserRequest();
        regularUser.setId(1L);
        regularUser.setUsername("alice");
        regularUser.setRole(Role.USER);

        adminUser = new UserRequest();
        adminUser.setId(99L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setEmail("admin@test.com");
        adminUser.setFullName("Admin User");
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_returnsMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(regularUser, adminUser));

        List<UserResponse> result = adminService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
        assertThat(result.get(1).getUsername()).isEqualTo("admin");
        assertThat(result.get(1).getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    void getAllUsers_emptyList_returnsEmpty() {
        when(userRepository.findAll()).thenReturn(List.of());
        assertThat(adminService.getAllUsers()).isEmpty();
    }

    // --- deleteUser ---

    @Test
    void deleteUser_regularUser_deletesAllAssociatedData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        adminService.deleteUser(1L);

        verify(transactionRepository).deleteByUser(regularUser);
        verify(budgetGoalRepository).deleteByUser(regularUser);
        verify(emiLoanRepository).deleteByUser(regularUser);
        verify(recurringTransactionRepository).deleteByUser(regularUser);
        verify(savingsGoalRepository).deleteByUser(regularUser);
        verify(refreshTokenRepository).deleteByUser(regularUser);
        verify(userRepository).delete(regularUser);
    }

    @Test
    void deleteUser_notFound_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void deleteUser_adminAccount_throwsForbidden() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.deleteUser(99L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Admin accounts cannot be deleted");
    }

    // --- getAllTransactions ---

    @Test
    void getAllTransactions_returnsMappedList() {
        TransactionRequest t = new TransactionRequest();
        t.setId(1L);
        t.setType("INCOME");
        t.setAmount(500.0);
        t.setCategory("Salary");
        t.setDate(LocalDate.now());
        t.setUser(regularUser);
        when(transactionRepository.findAllByOrderByDateDesc()).thenReturn(List.of(t));

        List<TransactionResponse> result = adminService.getAllTransactions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("INCOME");
    }

    // --- deleteTransaction ---

    @Test
    void deleteTransaction_found_deletes() {
        TransactionRequest t = new TransactionRequest();
        t.setId(5L);
        t.setUser(regularUser);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));

        adminService.deleteTransaction(5L);

        verify(transactionRepository).delete(t);
    }

    @Test
    void deleteTransaction_notFound_throwsResourceNotFound() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteTransaction(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
