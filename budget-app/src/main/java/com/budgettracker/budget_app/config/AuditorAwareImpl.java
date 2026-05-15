package com.budgettracker.budget_app.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(
                Objects.requireNonNull(SecurityContextHolder.getContext()
                                .getAuthentication())
                        .getName()
        );
    }
}
