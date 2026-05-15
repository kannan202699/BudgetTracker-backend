package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.BudgetGoal;
import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetGoalRepository extends JpaRepository<BudgetGoal, Long> {

    List<BudgetGoal> findByUser(UserRequest user);

    Optional<BudgetGoal> findByUserAndCategory(UserRequest user, String category);

    @Modifying
    void deleteByUserAndCategory(UserRequest user, String category);
}
