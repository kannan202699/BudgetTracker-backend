package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.PasswordResetToken;
import com.budgettracker.budget_app.requestdto.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    void deleteByUser(UserRequest user);

}
