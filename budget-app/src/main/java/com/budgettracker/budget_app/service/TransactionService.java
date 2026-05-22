package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.TransactionRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.TransactionResponse;
import com.budgettracker.budget_app.util.ResponseMapper;
import com.budgettracker.budget_app.util.Role;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for creating, reading, updating, and deleting transactions.
 */
@Service
@Slf4j
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(@NonNull UserRepository userRepository,
                              @NonNull TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Saves a new transaction attributed to the current user.
     */
    public TransactionResponse addTransaction(TransactionRequest transactionRequest) {
        log.debug("addTransaction - resolving current user");
        UserRequest user = getCurrentUser();

        log.debug("addTransaction - saving transaction: type={}, amount={}, category={}, user={}",
                transactionRequest.getType(), transactionRequest.getAmount(),
                transactionRequest.getCategory(), user.getUsername());
        transactionRequest.setUser(user);
        transactionRepository.save(transactionRequest);

        log.info("addTransaction - transaction saved: type={}, amount={}, category={}, user={}",
                transactionRequest.getType(), transactionRequest.getAmount(),
                transactionRequest.getCategory(), user.getUsername());
        return ResponseMapper.toTransactionResponse(transactionRequest);
    }

    /**
     * Returns transactions for the current authenticated user only, regardless of role.
     */
    public List<TransactionResponse> getAllTransactions() {
        log.debug("getAllTransactions - resolving current user");
        UserRequest user = getCurrentUser();

        log.debug("getAllTransactions - fetching own transactions for: {}", user.getUsername());
        List<TransactionRequest> transactions = transactionRepository.findByUser(user, Sort.by("date").descending());

        log.info("getAllTransactions - fetched {} transaction(s) for user: {}", transactions.size(), user.getUsername());
        return transactions.stream().map(ResponseMapper::toTransactionResponse).collect(Collectors.toList());
    }

    /**
     * Returns a paginated slice of the current user's own transactions, regardless of role.
     */
    public Page<TransactionResponse> getTransactions(int pageNum, int size) {
        log.debug("getTransactions - resolving current user, page={}, size={}", pageNum, size);
        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("date").descending());
        UserRequest user = getCurrentUser();

        log.debug("getTransactions - fetching own paginated transactions for: {}", user.getUsername());
        Page<TransactionRequest> page = transactionRepository.findByUser(user, pageable);

        log.info("getTransactions - returned {} transaction(s) on page {} for user: {}",
                page.getNumberOfElements(), pageNum, user.getUsername());
        return page.map(ResponseMapper::toTransactionResponse);
    }

    /**
     * Updates a transaction; only the owning user may edit it.
     */
    public TransactionResponse updateTransaction(TransactionRequest transactionRequest, Long id) {
        log.debug("updateTransaction - fetching transaction id={}", id);
        TransactionRequest existing = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("updateTransaction - transaction not found: id={}", id);
                    return new ResourceNotFoundException("Transaction not found with id: " + id);
                });

        log.debug("updateTransaction - resolving current user");
        UserRequest currentUser = getCurrentUser();

        if (!existing.getUser().getId().equals(currentUser.getId())) {
            log.warn("updateTransaction - forbidden: user={} attempted to update transaction owned by userId={}",
                    currentUser.getUsername(), existing.getUser().getId());
            throw new ForbiddenException("You cannot update this transaction");
        }

        log.debug("updateTransaction - applying changes: type={}, amount={}, category={}, date={}",
                transactionRequest.getType(), transactionRequest.getAmount(),
                transactionRequest.getCategory(), transactionRequest.getDate());
        existing.setAmount(transactionRequest.getAmount());
        existing.setCategory(transactionRequest.getCategory());
        existing.setDescription(transactionRequest.getDescription());
        existing.setDate(transactionRequest.getDate());
        existing.setType(transactionRequest.getType());
        transactionRepository.save(existing);

        log.info("updateTransaction - transaction id={} updated by user: {}", id, currentUser.getUsername());
        return ResponseMapper.toTransactionResponse(existing);
    }

    /**
     * Deletes a transaction; admins may delete any, users may only delete their own.
     */
    public void deleteTransaction(Long id) {
        log.debug("deleteTransaction - fetching transaction id={}", id);
        TransactionRequest transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("deleteTransaction - transaction not found: id={}", id);
                    return new ResourceNotFoundException("Transaction not found with id: " + id);
                });

        log.debug("deleteTransaction - resolving current user");
        UserRequest currentUser = getCurrentUser();

        if (currentUser.getRole().equals(Role.USER)
                && !transaction.getUser().getId().equals(currentUser.getId())) {
            log.warn("deleteTransaction - forbidden: user={} attempted to delete transaction owned by userId={}",
                    currentUser.getUsername(), transaction.getUser().getId());
            throw new ForbiddenException("You cannot delete this transaction");
        }

        transactionRepository.deleteById(id);
        log.info("deleteTransaction - transaction id={} deleted by user: {}", id, currentUser.getUsername());
    }

    /**
     * Resolves the authenticated user from the security context; throws if not found.
     */
    public UserRequest getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("getCurrentUser - resolving user from security context: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("getCurrentUser - authenticated user not found in DB: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

}
