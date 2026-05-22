package com.budgettracker.budget_app.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Supplies the currently authenticated username as the JPA auditor for @CreatedBy/@LastModifiedBy fields.
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    /**
     * Returns the username of the currently authenticated principal from the security context.
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(
                Objects.requireNonNull(SecurityContextHolder.getContext()
                                .getAuthentication())
                        .getName()
        );
    }
}
