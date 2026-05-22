package com.budgettracker.budget_app.security;

import com.budgettracker.budget_app.util.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 32-byte key encoded as Base64
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckp3dFRlc3RpbmcxMjM0NTY3OA==";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "tokenValidityMs", 3_600_000L);
    }

    @Test
    void generateToken_containsUsernameAndRole() {
        String token = jwtUtil.generateToken("alice", Role.USER);
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void generateToken_adminRole() {
        String token = jwtUtil.generateToken("admin", Role.ADMIN);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("alice", Role.USER);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("alice", Role.USER);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_randomString_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void extractAllClaims_returnsCorrectSubject() {
        String token = jwtUtil.generateToken("bob", Role.USER);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("bob");
    }

    @Test
    void extractAllClaims_invalidToken_throwsException() {
        assertThatThrownBy(() -> jwtUtil.extractAllClaims("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }
}
