package com.budgettracker.budget_app.requestdto;

import com.budgettracker.budget_app.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email verification is required — please verify your email first")
    private String verifiedToken;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 100, message = "Email must be 100 characters or less")
    private String email;

    @Pattern(
        regexp = "^$|^\\+?[\\d\\s\\-()/]{7,20}$",
        message = "Invalid phone number (7–20 digits, optional +)"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;
}
