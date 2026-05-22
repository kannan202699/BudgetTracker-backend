package com.budgettracker.budget_app.repository;

import com.budgettracker.budget_app.requestdto.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByEmail(String email);

    Optional<EmailVerificationToken> findByVerifiedToken(String verifiedToken);

    @Transactional
    void deleteByEmail(String email);

}
