package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserRequest, Long> {
    Optional<UserRequest> findByUsername(String username);
}
