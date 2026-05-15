package com.budgettracker.budget_app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext ctx) {
        if (password == null || password.isBlank()) return false;
        if (password.length() < 8 || password.length() > 100) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[a-z].*")) return false;
        if (!password.matches(".*\\d.*")) return false;
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>\\-_=+\\[\\]\\\\;'/].*")) return false;
        return true;
    }
}
