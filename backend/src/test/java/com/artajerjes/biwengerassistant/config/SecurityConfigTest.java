package com.artajerjes.biwengerassistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Test
    void shouldEncodePassword() {
        String rawPassword = "test-password";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword)
                .isNotEqualTo(rawPassword);

        assertThat(passwordEncoder.matches(
                rawPassword,
                encodedPassword))
                .isTrue();
    }

    @Test
    void shouldRejectWrongPassword() {
        String encodedPassword = passwordEncoder.encode("correct-password");

        assertThat(passwordEncoder.matches(
                "wrong-password",
                encodedPassword))
                .isFalse();
    }
}