package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.repository.BudgetGoalRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.BudgetGoal;
import com.budgettracker.budget_app.requestdto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages per-category spending budget goals for each user.
 */
@Service
@Slf4j
public class BudgetGoalService {

    private final BudgetGoalRepository budgetGoalRepository;
    private final UserRepository userRepository;

    public BudgetGoalService(BudgetGoalRepository budgetGoalRepository,
                             UserRepository userRepository) {
        this.budgetGoalRepository = budgetGoalRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns a category-to-amount map of all budget goals for the current user.
     */
    public Map<String, Double> getGoals() {
        log.debug("getGoals - resolving current user");
        UserRequest user = currentUser();

        log.debug("getGoals - fetching budget goals for user: {}", user.getUsername());
        Map<String, Double> map = new LinkedHashMap<>();
        budgetGoalRepository.findByUser(user).forEach(g -> map.put(g.getCategory(), g.getAmount()));

        log.info("getGoals - returning {} goal(s) for user: {}", map.size(), user.getUsername());
        return map;
    }

    /**
     * Creates or updates the budget goal for the given category.
     */
    @Transactional
    public void setGoal(String category, double amount) {
        log.debug("setGoal - resolving current user");
        UserRequest user = currentUser();

        log.debug("setGoal - looking up existing goal: user={}, category={}", user.getUsername(), category);
        BudgetGoal goal = budgetGoalRepository
                .findByUserAndCategory(user, category)
                .orElse(BudgetGoal.builder().user(user).category(category).build());

        boolean isNew = goal.getId() == null;
        log.debug("setGoal - {} goal for user={}, category={}, amount={}",
                isNew ? "creating new" : "updating existing", user.getUsername(), category, amount);

        goal.setAmount(amount);
        budgetGoalRepository.save(goal);

        log.info("setGoal - budget goal {}: user={}, category={}, amount={}",
                isNew ? "created" : "updated", user.getUsername(), category, amount);
    }

    /**
     * Removes the budget goal for the specified category.
     */
    @Transactional
    public void deleteGoal(String category) {
        log.debug("deleteGoal - resolving current user");
        UserRequest user = currentUser();

        log.debug("deleteGoal - deleting goal: user={}, category={}", user.getUsername(), category);
        budgetGoalRepository.deleteByUserAndCategory(user, category);

        log.info("deleteGoal - budget goal removed: user={}, category={}", user.getUsername(), category);
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
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }
}
