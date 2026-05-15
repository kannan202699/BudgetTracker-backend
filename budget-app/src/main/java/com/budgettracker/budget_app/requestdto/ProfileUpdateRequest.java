package com.budgettracker.budget_app.requestdto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    // ^$ allows empty string (clearing the field); rest validates a proper email
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Pattern(
        regexp = "^$|^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$",
        message = "Invalid email format"
    )
    private String email;

    @Size(max = 60, message = "Full name must not exceed 60 characters")
    @Pattern(
        regexp = "^$|^[a-zA-Z\\s'\\-]{2,60}$",
        message = "Full name can only contain letters, spaces, hyphens, and apostrophes"
    )
    private String fullName;

    @Pattern(
        regexp = "^$|^\\+?[\\d\\s\\-()/]{7,20}$",
        message = "Invalid phone number (7-20 digits)"
    )
    private String phone;
}
