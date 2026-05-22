package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.*;
import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.util.ResponseMapper;
import com.budgettracker.budget_app.util.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only service for managing users and all transactions across the system.
 */
@Service
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetGoalRepository budgetGoalRepository;
    private final EmiLoanRepository emiLoanRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public AdminService(UserRepository userRepository,
                        TransactionRepository transactionRepository,
                        BudgetGoalRepository budgetGoalRepository,
                        EmiLoanRepository emiLoanRepository,
                        RecurringTransactionRepository recurringTransactionRepository,
                        SavingsGoalRepository savingsGoalRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetGoalRepository = budgetGoalRepository;
        this.emiLoanRepository = emiLoanRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * Returns a summary list of all registered users.
     */
    public List<UserResponse> getAllUsers() {
        log.debug("getAllUsers - querying all users from repository");
        List<UserResponse> users = userRepository.findAll().stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .role(u.getRole().name())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .phone(u.getPhone())
                        .profilePicture(u.getProfilePicture())
                        .build())
                .collect(Collectors.toList());
        log.info("getAllUsers - returning {} user(s)", users.size());
        return users;
    }

    /**
     * Deletes a user and all their associated data; admin accounts cannot be deleted.
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("deleteUser - fetching user by id={}", userId);
        UserRequest user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("deleteUser - user not found: id={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        if (user.getRole().equals(Role.ADMIN)) {
            log.warn("deleteUser - attempted to delete admin account: username={}", user.getUsername());
            throw new ForbiddenException("Admin accounts cannot be deleted");
        }

        log.info("deleteUser - starting full data deletion for user: {}", user.getUsername());

        log.debug("deleteUser - deleting transactions for user: {}", user.getUsername());
        transactionRepository.deleteByUser(user);

        log.debug("deleteUser - deleting budget goals for user: {}", user.getUsername());
        budgetGoalRepository.deleteByUser(user);

        log.debug("deleteUser - deleting EMI loans for user: {}", user.getUsername());
        emiLoanRepository.deleteByUser(user);

        log.debug("deleteUser - deleting recurring transactions for user: {}", user.getUsername());
        recurringTransactionRepository.deleteByUser(user);

        log.debug("deleteUser - deleting savings goals for user: {}", user.getUsername());
        savingsGoalRepository.deleteByUser(user);

        log.debug("deleteUser - deleting refresh tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);

        log.debug("deleteUser - deleting password reset tokens for user: {}", user.getUsername());
        passwordResetTokenRepository.deleteByUser(user);

        log.debug("deleteUser - deleting user record: {}", user.getUsername());
        userRepository.delete(user);

        log.info("deleteUser - user {} and all associated data deleted successfully", user.getUsername());
    }

    /**
     * Returns all transactions in the system ordered by date descending.
     */
    public List<TransactionResponse> getAllTransactions() {
        log.debug("getAllTransactions - querying all transactions from repository");
        List<TransactionResponse> transactions = transactionRepository.findAllByOrderByDateDesc()
                .stream()
                .map(ResponseMapper::toTransactionResponse)
                .collect(Collectors.toList());
        log.info("getAllTransactions - returning {} transaction(s)", transactions.size());
        return transactions;
    }

    /**
     * Deletes a specific transaction by ID regardless of ownership.
     */
    public void deleteTransaction(Long id) {
        log.debug("deleteTransaction - fetching transaction id={}", id);
        TransactionRequest transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("deleteTransaction - transaction not found: id={}", id);
                    return new ResourceNotFoundException("Transaction not found with id: " + id);
                });

        log.debug("deleteTransaction - deleting transaction id={}, owner={}", id, transaction.getUser().getUsername());
        transactionRepository.delete(transaction);
        log.info("deleteTransaction - transaction id={} deleted by admin", id);
    }
}
