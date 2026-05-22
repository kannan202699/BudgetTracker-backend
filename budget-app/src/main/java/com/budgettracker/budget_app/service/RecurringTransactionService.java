package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.RecurringTransactionRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.RecurringTransaction;
import com.budgettracker.budget_app.requestdto.RecurringTransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.RecurringTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages recurring transaction templates (schedules) for each user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository repo;
    private final UserRepository userRepository;

    /**
     * Returns all recurring transaction templates for the current user.
     */
    public List<RecurringTransactionResponse> getAll() {
        log.debug("getAll - resolving current user");
        UserRequest user = currentUser();

        log.debug("getAll - fetching recurring transactions for user: {}", user.getUsername());
        List<RecurringTransactionResponse> list = repo.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.info("getAll - returning {} recurring transaction(s) for user: {}", list.size(), user.getUsername());
        return list;
    }

    /**
     * Creates a new recurring transaction template for the current user.
     */
    @Transactional
    public RecurringTransactionResponse create(RecurringTransactionRequest req) {
        log.debug("create - resolving current user");
        UserRequest user = currentUser();

        log.debug("create - building recurring transaction: type={}, amount={}, category={}, frequency={}, user={}",
                req.getType(), req.getAmount(), req.getCategory(), req.getFrequency(), user.getUsername());

        RecurringTransaction rt = RecurringTransaction.builder()
                .user(user).type(req.getType()).amount(req.getAmount())
                .category(req.getCategory()).description(req.getDescription())
                .frequency(req.getFrequency()).dayOfMonth(req.getDayOfMonth())
                .active(req.getActive() != null ? req.getActive() : true).build();

        RecurringTransactionResponse response = toResponse(repo.save(rt));
        log.info("create - recurring transaction created: id={}, type={}, category={}, user={}",
                response.getId(), response.getType(), response.getCategory(), user.getUsername());
        return response;
    }

    /**
     * Updates an existing recurring transaction owned by the current user.
     */
    @Transactional
    public RecurringTransactionResponse update(Long id, RecurringTransactionRequest req) {
        log.debug("update - fetching recurring transaction id={}", id);
        RecurringTransaction rt = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("update - recurring transaction not found: id={}", id);
                    return new ResourceNotFoundException("Recurring transaction not found");
                });

        log.debug("update - verifying ownership for id={}", id);
        UserRequest user = currentUser();
        if (!rt.getUser().getId().equals(user.getId())) {
            log.warn("update - access denied: user={} attempted to update recurring transaction id={} owned by userId={}",
                    user.getUsername(), id, rt.getUser().getId());
            throw new ForbiddenException("Access denied");
        }

        log.debug("update - applying changes: type={}, amount={}, category={}, frequency={}, active={}",
                req.getType(), req.getAmount(), req.getCategory(), req.getFrequency(), req.getActive());
        rt.setType(req.getType());
        rt.setAmount(req.getAmount());
        rt.setCategory(req.getCategory());
        rt.setDescription(req.getDescription());
        rt.setFrequency(req.getFrequency());
        rt.setDayOfMonth(req.getDayOfMonth());
        if (req.getActive() != null) rt.setActive(req.getActive());

        RecurringTransactionResponse response = toResponse(repo.save(rt));
        log.info("update - recurring transaction id={} updated, user={}", id, user.getUsername());
        return response;
    }

    /**
     * Deletes an existing recurring transaction owned by the current user.
     */
    @Transactional
    public void delete(Long id) {
        log.debug("delete - fetching recurring transaction id={}", id);
        RecurringTransaction rt = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("delete - recurring transaction not found: id={}", id);
                    return new ResourceNotFoundException("Recurring transaction not found");
                });

        log.debug("delete - verifying ownership for id={}", id);
        UserRequest user = currentUser();
        if (!rt.getUser().getId().equals(user.getId())) {
            log.warn("delete - access denied: user={} attempted to delete recurring transaction id={} owned by userId={}",
                    user.getUsername(), id, rt.getUser().getId());
            throw new ForbiddenException("Access denied");
        }

        repo.deleteById(id);
        log.info("delete - recurring transaction id={} deleted, user={}", id, user.getUsername());
    }

    /**
     * Maps a RecurringTransaction entity to its response DTO.
     */
    private RecurringTransactionResponse toResponse(RecurringTransaction rt) {
        log.debug("toResponse - mapping recurring transaction id={}", rt.getId());
        return RecurringTransactionResponse.builder()
                .id(rt.getId()).type(rt.getType())
                .amount(rt.getAmount()).category(rt.getCategory()).description(rt.getDescription())
                .frequency(rt.getFrequency()).dayOfMonth(rt.getDayOfMonth()).active(rt.getActive()).build();
    }

    /**
     * Resolves the authenticated user from the security context; throws if not found.
     */
    private UserRequest currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("currentUser - resolving user from security context: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("currentUser - authenticated user not found in DB: {}", username);
                    return new UsernameNotFoundException("User not found");
                });
    }
}
