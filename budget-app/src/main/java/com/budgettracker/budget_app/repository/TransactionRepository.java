package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.TransactionRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRequest, Long> {

    Page<TransactionRequest> findByUser(UserRequest user, Pageable pageable);

    List<TransactionRequest> findByUser(UserRequest user, Sort sort);

    List<TransactionRequest> findAllByOrderByDateDesc();

    void deleteByUser(UserRequest user);
}
