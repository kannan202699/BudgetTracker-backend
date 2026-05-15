package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.AuthRequest;
import com.budgettracker.budget_app.requestdto.RefreshToken;
import com.budgettracker.budget_app.requestdto.RegisterRequest;
import com.budgettracker.budget_app.requestdto.UserRequest;
import com.budgettracker.budget_app.responsedto.AuthResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.util.Role;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthService(@NonNull JwtUtil jwtUtil,
                       @NonNull UserRepository userRepository,
                       @NonNull PasswordEncoder passwordEncoder,
                       @NonNull RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public void saveUser(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username is already taken");
        }
        UserRequest user = new UserRequest();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);
        log.info("New user registered: {}", username);
    }

    public AuthResponse generateAuthToken(AuthRequest request) {
        UserRequest user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new UnAuthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new UnAuthorizedException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login successful for user: {} with role: {}", user.getUsername(), user.getRole());
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole().name())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }
}
