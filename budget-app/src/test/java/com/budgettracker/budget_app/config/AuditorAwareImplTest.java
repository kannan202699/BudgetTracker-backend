package com.budgettracker.budget_app.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditorAwareImplTest {

    private final AuditorAwareImpl auditorAware = new AuditorAwareImpl();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_withAuthenticatedUser_returnsUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null)
        );

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent().contains("alice");
    }

    @Test
    void getCurrentAuditor_withNullAuthentication_throwsNPE() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> auditorAware.getCurrentAuditor())
                .isInstanceOf(NullPointerException.class);
    }
}
