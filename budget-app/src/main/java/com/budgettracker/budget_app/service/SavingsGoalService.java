package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.ForbiddenException;
import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.SavingsGoalRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.SavingsGoal;
import com.budgettracker.budget_app.requestdto.SavingsGoalRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.SavingsGoalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages savings goals (target amount, saved amount, deadline) for each user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsGoalService {

    private final SavingsGoalRepository repo;
    private final UserRepository userRepository;

    /**
     * Returns all savings goals belonging to the current user.
     */
    public List<SavingsGoalResponse> getAll() {
        log.debug("getAll - resolving current user");
        UserRequest user = currentUser();

        log.debug("getAll - fetching savings goals for user: {}", user.getUsername());
        List<SavingsGoalResponse> goals = repo.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.info("getAll - returning {} savings goal(s) for user: {}", goals.size(), user.getUsername());
        return goals;
    }

    /**
     * Creates a new savings goal for the current user.
     */
    @Transactional
    public SavingsGoalResponse create(SavingsGoalRequest req) {
        log.debug("create - resolving current user");
        UserRequest user = currentUser();

        log.debug("create - building savings goal: title={}, target={}, saved={}, deadline={}, user={}",
                req.getTitle(), req.getTargetAmount(), req.getSavedAmount(), req.getDeadline(), user.getUsername());

        SavingsGoal goal = SavingsGoal.builder()
                .user(user).title(req.getTitle())
                .targetAmount(req.getTargetAmount()).savedAmount(req.getSavedAmount())
                .deadline(req.getDeadline()).build();

        SavingsGoalResponse response = toResponse(repo.save(goal));
        log.info("create - savings goal created: id={}, title={}, target={}, user={}",
                response.getId(), response.getTitle(), response.getTargetAmount(), user.getUsername());
        return response;
    }

    /**
     * Updates an existing savings goal owned by the current user.
     */
    @Transactional
    public SavingsGoalResponse update(Long id, SavingsGoalRequest req) {
        log.debug("update - fetching savings goal id={}", id);
        SavingsGoal goal = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("update - savings goal not found: id={}", id);
                    return new ResourceNotFoundException("Savings goal not found");
                });

        log.debug("update - verifying ownership for id={}", id);
        UserRequest user = currentUser();
        if (!goal.getUser().getId().equals(user.getId())) {
            log.warn("update - access denied: user={} attempted to update savings goal id={} owned by userId={}",
                    user.getUsername(), id, goal.getUser().getId());
            throw new ForbiddenException("Access denied");
        }

        log.debug("update - applying changes: title={}, target={}, saved={}, deadline={}",
                req.getTitle(), req.getTargetAmount(), req.getSavedAmount(), req.getDeadline());
        goal.setTitle(req.getTitle());
        goal.setTargetAmount(req.getTargetAmount());
        goal.setSavedAmount(req.getSavedAmount());
        goal.setDeadline(req.getDeadline());

        SavingsGoalResponse response = toResponse(repo.save(goal));
        log.info("update - savings goal id={} updated, user={}", id, user.getUsername());
        return response;
    }

    /**
     * Deletes an existing savings goal owned by the current user.
     */
    @Transactional
    public void delete(Long id) {
        log.debug("delete - fetching savings goal id={}", id);
        SavingsGoal goal = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("delete - savings goal not found: id={}", id);
                    return new ResourceNotFoundException("Savings goal not found");
                });

        log.debug("delete - verifying ownership for id={}", id);
        UserRequest user = currentUser();
        if (!goal.getUser().getId().equals(user.getId())) {
            log.warn("delete - access denied: user={} attempted to delete savings goal id={} owned by userId={}",
                    user.getUsername(), id, goal.getUser().getId());
            throw new ForbiddenException("Access denied");
        }

        repo.deleteById(id);
        log.info("delete - savings goal id={} deleted, user={}", id, user.getUsername());
    }

    /**
     * Maps a SavingsGoal entity to its response DTO.
     */
    private SavingsGoalResponse toResponse(SavingsGoal g) {
        log.debug("toResponse - mapping savings goal id={}", g.getId());
        return SavingsGoalResponse.builder()
                .id(g.getId()).title(g.getTitle())
                .targetAmount(g.getTargetAmount()).savedAmount(g.getSavedAmount())
                .deadline(g.getDeadline()).build();
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
