package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.RecurringTransaction;
import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUser(UserRequest user);

    @Modifying
    void deleteByUser(UserRequest user);

}
