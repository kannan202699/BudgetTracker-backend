package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.TransactionRepository;
import com.budgettracker.budget_app.repository.UserRepository;
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

@Service
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public AdminService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .role(u.getRole().name())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .phone(u.getPhone())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        UserRequest user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole().equals(Role.ADMIN)) {
            throw new ForbiddenException("Admin accounts cannot be deleted");
        }

        transactionRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("Deleted user {} and their transactions", user.getUsername());
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllByOrderByDateDesc()
                .stream()
                .map(ResponseMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    public void deleteTransaction(Long id) {
        TransactionRequest transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        transactionRepository.delete(transaction);
        log.info("Admin deleted transaction id: {}", id);
    }
}
