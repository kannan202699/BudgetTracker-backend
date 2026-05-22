package com.budgettracker.budget_app.responsedto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private String email;
    private String fullName;
    private String phone;
    private String profilePicture;
}
