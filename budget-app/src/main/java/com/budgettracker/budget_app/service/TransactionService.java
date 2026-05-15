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

    public TransactionResponse addTransaction(TransactionRequest transactionRequest) {
        UserRequest user = getCurrentUser();
        transactionRequest.setUser(user);
        transactionRepository.save(transactionRequest);
        log.info("Transaction added for user: {}", user.getUsername());
        return ResponseMapper.toTransactionResponse(transactionRequest);
    }

    public List<TransactionResponse> getAllTransactions() {
        UserRequest user = getCurrentUser();
        List<TransactionRequest> transactions;
        if (user.getRole().equals(Role.ADMIN)) {
            transactions = transactionRepository.findAllByOrderByDateDesc();
        } else {
            transactions = transactionRepository.findByUser(user, Sort.by("date").descending());
        }
        log.info("Fetched all transactions for user: {}", user.getUsername());
        return transactions.stream().map(ResponseMapper::toTransactionResponse).collect(Collectors.toList());
    }

    public Page<TransactionResponse> getTransactions(int pageNum, int size) {
        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("date").descending());
        UserRequest user = getCurrentUser();
        Page<TransactionRequest> page = user.getRole().equals(Role.ADMIN)
                ? transactionRepository.findAll(pageable)
                : transactionRepository.findByUser(user, pageable);
        return page.map(ResponseMapper::toTransactionResponse);
    }

    public TransactionResponse updateTransaction(TransactionRequest transactionRequest, Long id) {
        TransactionRequest existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        UserRequest currentUser = getCurrentUser();
        if (!existing.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot update this transaction");
        }

        existing.setAmount(transactionRequest.getAmount());
        existing.setCategory(transactionRequest.getCategory());
        existing.setDescription(transactionRequest.getDescription());
        existing.setDate(transactionRequest.getDate());
        existing.setType(transactionRequest.getType());

        transactionRepository.save(existing);
        log.info("Transaction {} updated by user: {}", id, currentUser.getUsername());
        return ResponseMapper.toTransactionResponse(existing);
    }

    public void deleteTransaction(Long id) {
        TransactionRequest transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        UserRequest currentUser = getCurrentUser();
        if (currentUser.getRole().equals(Role.USER)
                && !transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot delete this transaction");
        }
        transactionRepository.deleteById(id);
        log.info("Transaction {} deleted by user: {}", id, currentUser.getUsername());
    }

    public UserRequest getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
