package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.ProfileUpdateRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        UserRequest user = getCurrentUser();
        return ResponseEntity.ok(toUserResponse(user));
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        UserRequest user = getCurrentUser();
        // Convert empty string to null so the DB stores null instead of ""
        user.setEmail(blankToNull(request.getEmail()));
        user.setFullName(blankToNull(request.getFullName()));
        user.setPhone(blankToNull(request.getPhone()));
        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getUsername());
        return ResponseEntity.ok(toUserResponse(user));
    }

    private UserRequest getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toUserResponse(UserRequest user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .build();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
