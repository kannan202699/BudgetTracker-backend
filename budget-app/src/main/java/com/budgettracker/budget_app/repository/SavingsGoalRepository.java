package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.SavingsGoal;
import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUser(UserRequest user);

    @Modifying
    void deleteByUser(UserRequest user);

}
