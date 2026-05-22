package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.EmiLoan;
import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface EmiLoanRepository extends JpaRepository<EmiLoan, Long> {

    List<EmiLoan> findByUser(UserRequest user);

    @Modifying
    void deleteByUser(UserRequest user);

}
