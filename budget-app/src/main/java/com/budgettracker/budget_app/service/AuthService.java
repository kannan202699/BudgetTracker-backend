package com.budgettracker.budget_app.service;

import com.budgettracker.budget_app.exception.UnAuthorizedException;
import com.budgettracker.budget_app.repository.EmailVerificationTokenRepository;
import com.budgettracker.budget_app.repository.PasswordResetTokenRepository;
import com.budgettracker.budget_app.repository.UserRepository;
import com.budgettracker.budget_app.requestdto.*;
import com.budgettracker.budget_app.responsedto.AuthResponse;
import com.budgettracker.budget_app.security.JwtUtil;
import com.budgettracker.budget_app.util.Role;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles registration (with email OTP verification), login, token refresh, and password management.
 */
@Service
@Slf4j
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int VERIFIED_EXPIRY_MINUTES = 30;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Autowired
    public AuthService(@NonNull JwtUtil jwtUtil,
                       @NonNull UserRepository userRepository,
                       @NonNull PasswordEncoder passwordEncoder,
                       @NonNull RefreshTokenService refreshTokenService,
                       @NonNull PasswordResetTokenRepository passwordResetTokenRepository,
                       @NonNull EmailVerificationTokenRepository emailVerificationTokenRepository,
                       @NonNull EmailService emailService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
    }

    // ── Email Verification ──────────────────────────────────────────────────

    /**
     * Sends a 6-digit OTP to the given email.
     * Enforces a 60-second resend cooldown and rejects already-registered emails.
     */
    @Transactional
    public void sendEmailVerification(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("This email is already registered. Please sign in.");
        }

        Optional<EmailVerificationToken> existing =
                emailVerificationTokenRepository.findByEmail(normalizedEmail);

        if (existing.isPresent()) {
            long secondsSinceSent = ChronoUnit.SECONDS.between(existing.get().getCreatedAt(), Instant.now());
            if (secondsSinceSent < RESEND_COOLDOWN_SECONDS) {
                long wait = RESEND_COOLDOWN_SECONDS - secondsSinceSent;
                throw new RuntimeException("Please wait " + wait + " seconds before requesting another code.");
            }
            emailVerificationTokenRepository.deleteByEmail(normalizedEmail);
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        emailVerificationTokenRepository.save(
                EmailVerificationToken.builder()
                        .email(normalizedEmail)
                        .otp(otp)
                        .createdAt(Instant.now())
                        .expiryDate(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                        .build()
        );

        emailService.sendEmailVerificationOtp(normalizedEmail, otp);
        log.info("sendEmailVerification - OTP sent to: {}", normalizedEmail);
    }

    /**
     * Confirms the OTP. On success, marks the record verified and returns a short-lived
     * verifiedToken that must be included in the registration request.
     */
    @Transactional
    public String verifyEmailOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();

        EmailVerificationToken record = emailVerificationTokenRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No verification code found. Please request a new one."));

        if (record.getExpiryDate().isBefore(Instant.now())) {
            emailVerificationTokenRepository.delete(record);
            throw new RuntimeException("Verification code has expired. Please request a new one.");
        }
        if (!record.getOtp().equals(otp.trim())) {
            throw new RuntimeException("Incorrect verification code. Please try again.");
        }

        String verifiedToken = UUID.randomUUID().toString();
        record.setVerified(true);
        record.setVerifiedToken(verifiedToken);
        record.setExpiryDate(Instant.now().plus(VERIFIED_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        emailVerificationTokenRepository.save(record);

        log.info("verifyEmailOtp - email verified: {}", normalizedEmail);
        return verifiedToken;
    }

    // ── Registration ────────────────────────────────────────────────────────

    /**
     * Validates the verifiedToken, checks uniqueness of username + email, then persists the user.
     */
    @Transactional
    public void saveUser(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String verifiedToken = request.getVerifiedToken().trim();

        // Validate email verification token
        EmailVerificationToken evToken = emailVerificationTokenRepository
                .findByVerifiedToken(verifiedToken)
                .orElseThrow(() -> new RuntimeException("Email not verified. Please complete email verification first."));

        if (!evToken.isVerified()) {
            throw new RuntimeException("Email not verified. Please complete email verification first.");
        }
        if (!evToken.getEmail().equals(email)) {
            throw new RuntimeException("Email does not match the verified address.");
        }
        if (evToken.getExpiryDate().isBefore(Instant.now())) {
            emailVerificationTokenRepository.delete(evToken);
            throw new RuntimeException("Verification session expired. Please verify your email again.");
        }

        // Uniqueness checks
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        UserRequest user = new UserRequest();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(evToken);
        log.info("saveUser - new user registered: {}", username);
    }

    // ── Login ────────────────────────────────────────────────────────────────

    /**
     * Accepts username OR email as identifier; returns JWT + refresh token on success.
     */
    public AuthResponse generateAuthToken(AuthRequest request) {
        String identifier = request.getUsername().trim();

        UserRequest user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier.toLowerCase())
                    .orElseThrow(() -> new UnAuthorizedException("Invalid credentials"));
        } else {
            user = userRepository.findByUsernameIgnoreCase(identifier)
                    .orElseThrow(() -> new UnAuthorizedException("Invalid credentials"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnAuthorizedException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("generateAuthToken - login successful: {}", user.getUsername());
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole().name())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    // ── Forgot / Reset Password ──────────────────────────────────────────────

    /**
     * Sends a one-time password reset link to the email; silently no-ops if the email is not registered.
     */
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<UserRequest> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.info("forgotPassword - email not registered (not revealing): {}", normalizedEmail);
            return;
        }

        UserRequest user = userOpt.get();
        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(
                PasswordResetToken.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                        .build()
        );

        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
        log.info("forgotPassword - reset token issued for: {}", normalizedEmail);
    }

    /**
     * Validates the reset token and updates the user's password, then revokes all refresh tokens.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (resetToken.isUsed()) throw new RuntimeException("Reset token has already been used");
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }

        UserRequest user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        refreshTokenService.deleteByUser(user);
        log.info("resetPassword - password updated for user: {}", user.getUsername());
    }

    // ── Change Password (authenticated user) ────────────────────────────────

    /**
     * Verifies the current password and replaces it; revokes all existing refresh tokens on success.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserRequest user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UnAuthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.deleteByUser(user);
        log.info("changePassword - password changed for user: {}", username);
    }

}
