package com.budgettracker.budget_app.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordConstraintValidator();
    }

    @Test
    void nullPassword_returnsFalse() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void blankPassword_returnsFalse() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @Test
    void tooShort_returnsFalse() {
        assertThat(validator.isValid("Ab1!", null)).isFalse();
    }

    @Test
    void tooLong_returnsFalse() {
        String pwd = "Aa1!" + "x".repeat(100);
        assertThat(validator.isValid(pwd, null)).isFalse();
    }

    @Test
    void noUppercase_returnsFalse() {
        assertThat(validator.isValid("abcdef1!", null)).isFalse();
    }

    @Test
    void noLowercase_returnsFalse() {
        assertThat(validator.isValid("ABCDEF1!", null)).isFalse();
    }

    @Test
    void noDigit_returnsFalse() {
        assertThat(validator.isValid("Abcdefg!", null)).isFalse();
    }

    @Test
    void noSpecialChar_returnsFalse() {
        assertThat(validator.isValid("Abcdefg1", null)).isFalse();
    }

    @Test
    void validPassword_returnsTrue() {
        assertThat(validator.isValid("Valid@Pass1", null)).isTrue();
    }

    @Test
    void exactlyEightCharsValid_returnsTrue() {
        assertThat(validator.isValid("Ab1!cdef", null)).isTrue();
    }

    @Test
    void exactly100CharsValid_returnsTrue() {
        String pwd = "Aa1!" + "x".repeat(96);
        assertThat(validator.isValid(pwd, null)).isTrue();
    }

    @Test
    void exactly101Chars_returnsFalse() {
        String pwd = "Aa1!" + "x".repeat(97);
        assertThat(validator.isValid(pwd, null)).isFalse();
    }
}
