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

    public Map<String, Double> getGoals() {
        UserRequest user = currentUser();
        Map<String, Double> map = new LinkedHashMap<>();
        budgetGoalRepository.findByUser(user)
                .forEach(g -> map.put(g.getCategory(), g.getAmount()));
        return map;
    }

    @Transactional
    public void setGoal(String category, double amount) {
        UserRequest user = currentUser();
        BudgetGoal goal = budgetGoalRepository
                .findByUserAndCategory(user, category)
                .orElse(BudgetGoal.builder().user(user).category(category).build());
        goal.setAmount(amount);
        budgetGoalRepository.save(goal);
        log.info("Budget goal set: user={} category={} amount={}", user.getUsername(), category, amount);
    }

    @Transactional
    public void deleteGoal(String category) {
        UserRequest user = currentUser();
        budgetGoalRepository.deleteByUserAndCategory(user, category);
        log.info("Budget goal cleared: user={} category={}", user.getUsername(), category);
    }

    private UserRequest currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
