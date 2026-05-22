package com.budgettracker.budget_app.controller;

import com.budgettracker.budget_app.exception.ResourceNotFoundException;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.ChangePasswordRequest;
import com.budgettracker.budget_app.requestdto.ProfileUpdateRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.UserResponse;
import com.budgettracker.budget_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for viewing and updating the authenticated user's profile.
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Autowired
    public UserController(@NonNull UserRepository userRepository,
                          @NonNull AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     */
    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        UserRequest user = getCurrentUser();
        log.info("GET /user/profile - fetching profile for user: {}", user.getUsername());
        UserResponse response = toUserResponse(user);
        log.debug("GET /user/profile - profile returned for user: {}, email={}", user.getUsername(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates editable profile fields (email, fullName, phone) for the current user.
     */
    @Operation(summary = "Update current user profile")
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        UserRequest user = getCurrentUser();
        log.info("PUT /user/profile - update profile request for user: {}", user.getUsername());
        log.debug("PUT /user/profile - fields being updated: email={}, fullName={}, phone={}",
                request.getEmail(), request.getFullName(), request.getPhone());
        user.setEmail(blankToNull(request.getEmail()));
        user.setFullName(blankToNull(request.getFullName()));
        user.setPhone(blankToNull(request.getPhone()));
        userRepository.save(user);
        log.info("PUT /user/profile - profile updated for user: {}", user.getUsername());
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Replaces the profile picture (base64 image) for the current user.
     */
    @Operation(summary = "Update profile picture")
    @PostMapping("/profile/avatar")
    public ResponseEntity<UserResponse> updateAvatar(@RequestBody Map<String, String> body) {
        UserRequest user = getCurrentUser();
        log.info("POST /user/profile/avatar - avatar update request for user: {}", user.getUsername());
        String imageData = body.get("imageData");
        boolean cleared = imageData == null || imageData.isBlank();
        user.setProfilePicture(cleared ? null : imageData);
        userRepository.save(user);
        log.info("POST /user/profile/avatar - avatar {} for user: {}",
                cleared ? "cleared" : "updated", user.getUsername());
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Changes the password for the currently authenticated user.
     * Invalidates all existing refresh tokens on success, requiring re-login on other devices.
     */
    @Operation(summary = "Change password for the current user")
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("PUT /user/change-password - password change request for user: {}", username);
        authService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
        log.info("PUT /user/change-password - password changed successfully for user: {}", username);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please sign in again on other devices."));
    }

    /**
     * Resolves the authenticated user entity from the security context.
     */
    private UserRequest getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Maps a UserRequest entity to a UserResponse DTO.
     */
    private UserResponse toUserResponse(UserRequest user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    /**
     * Returns null for blank/null strings, trimmed value otherwise.
     */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

}
