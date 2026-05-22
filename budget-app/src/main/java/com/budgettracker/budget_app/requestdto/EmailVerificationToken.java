package com.budgettracker.budget_app.requestdto;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    // Set after OTP is confirmed — required in RegisterRequest
    @Column(unique = true)
    private String verifiedToken;

    @Column(nullable = false)
    private Instant createdAt;

    // OTP expiry (10 min); extended to 30 min from now after OTP is confirmed
    @Column(nullable = false)
    private Instant expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;
}
