package com.artajerjes.biwengerassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AssistantUserDetailsServiceTest {

    private final AssistantUserRepository assistantUserRepository = mock(AssistantUserRepository.class);

    private final AssistantUserDetailsService service = new AssistantUserDetailsService(assistantUserRepository);

    @Test
    void shouldLoadEnabledAdminUser() {
        AssistantUser assistantUser = new AssistantUser(
                "diego",
                "hashed-password",
                AssistantRole.ADMIN,
                null);

        when(assistantUserRepository.findByUsernameIgnoreCase("diego"))
                .thenReturn(Optional.of(assistantUser));

        UserDetails result = service.loadUserByUsername("diego");

        assertThat(result.getUsername()).isEqualTo("diego");
        assertThat(result.getPassword()).isEqualTo("hashed-password");
        assertThat(result.isEnabled()).isTrue();

        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(assistantUserRepository.findByUsernameIgnoreCase("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}